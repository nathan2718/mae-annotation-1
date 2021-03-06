/*
 * MAE - Multi-purpose Annotation Environment
 *
 * Copyright Keigh Rim (krim@brandeis.edu)
 * Department of Computer Science, Brandeis University
 * Original program by Amber Stubbs (astubbs@cs.brandeis.edu)
 *
 * MAE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, @see <a href="http://www.gnu.org/licenses">http://www.gnu.org/licenses</a>.
 *
 * For feedback, reporting bugs, use the project repo on github
 * @see <a href="https://github.com/keighrim/mae-annotation">https://github.com/keighrim/mae-annotation</a>
 */

package mae;


import java.sql.*;
import java.util.*;

/**
 * TagDB is the class that handles all the calls to the 
 * SQLite database.  TagDB in MAE has two tables:
 *
 * these column design was originally from Amber
 * 1) extents, with columns: 
 *    1-location int(5), 
 *    2-element_name, 
 *    3-id
 * links table is redesign for multi-linking support
 * 2) links, with columns: 
 *    1-location int(5), 
 *    2-element_name, 
 *    3-id,  // common attribs by far
 *    4, 5,..., 4+(2*MaxArgs), 4+(2*MaxArgs)+1 - arg0, arg0_name, ...argN, argN_name
 *     
 * User-defined attribute information about the tags that are being 
 * created is not stored in the database; it exists only in the 
 * tables that are part of MaeGui.  Therefore if the program 
 * is closed without the file being saved, the tags cannot
 * be completely recovered from the database.
 * @author Amber Stubbs, Keigh Rim
 *
 */

class AnnotDB {
    // krim: class renamed corresponding MAI

    private PreparedStatement mExt2Insert;
    private PreparedStatement mLink2Insert;
    private Connection mConn;

    // integers for each column in the table
    final int LOC_COL = 1;
    final int NAME_COL = 2;
    final int ID_COL = 3;
    final int ARG0_COL = 4;
    private int mMaxArgs;

    /**
     * Clears out the database and creates the 
     * tables and PreparedStatements.
     * 
     */
    AnnotDB() {
        this(2);
    }
    
    AnnotDB(int maxArgs) {
        try{
            mMaxArgs = maxArgs; // default number of args is 2
            Class.forName("org.sqlite.JDBC");
            mConn = DriverManager.getConnection("jdbc:sqlite:tag.db");
            Statement stat = mConn.createStatement();
            stat.executeUpdate("DROP TABLE if exists extents;");
            stat.executeUpdate("CREATE TABLE extents (location INT(5), element_name, id);");
            stat.executeUpdate("DROP TABLE if exists links;");
            stat.executeUpdate("CREATE TABLE links (location INT(5), element_name, id);");
            for (int i=0;i<mMaxArgs;i++) {
                String colname = "arg"+i;
                stat.executeUpdate("ALTER TABLE links ADD '" + colname + "';");
                colname = "arg"+i+"_name";
                stat.executeUpdate("ALTER TABLE links ADD '" + colname + "';");
            }

            // init Extent DB table with null values
            mExt2Insert = mConn.prepareStatement("insert into extents values (?, ?, ?);");
            
            // init link DB table with nul values
            String nullArgs = "";
            for (int i=0;i<mMaxArgs;i++) {
                nullArgs += ", ?, ?";
            }
            mLink2Insert = mConn.prepareStatement("insert into links values (?, ?, ?" +
                    nullArgs + ");");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void printExtents(){
        System.out.println("Extents in DB:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from extents;");
            int i = 0;
            while (rs.next()) {
                if (i % 10 == 0) {
                    System.out.printf("%20s\t%20s\t%20s%n",
                            "location", "element", "id");
                }
                System.out.printf("%20s\t%20s\t%20s%n",
                        rs.getString("location"),
                        rs.getString("element_name"),
                        rs.getString("id"));
                i++;
            }
            rs.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // TODO this method needs to re-written from scratch:
    // right now, this method is not used at anywhere so leave it now
    public void printLinks(){
        System.out.println("Links in DB:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from links;");
            int i = 0;
            while (rs.next()) {
                if (i % 10 == 0) {
                    System.out.printf("%20s\t%20s\t%20s\t%20s\t%20s%%%\t%20s%n",
                            "id", "from", "f_name", "to", "t_name", "element");
                }
                System.out.printf("%20s\t%20s\t%20s%n",
                        rs.getString("id"),
                        rs.getString("fromid"),
                        rs.getString("from_name"),
                        rs.getString("toid"),
                        rs.getString("to_name"),
                        rs.getString("element_name"));
                i++;
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get all tags bound to a certain location and return types of those tags
     *  
     * @param loc the character offset of the location being looked at
     * @return ArrayList of strings containing the types of elements at a location
     * @throws Exception 
     */
    ArrayList<String> getElementsAtLoc(int loc)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = "select * from extents where location = " + loc + ";";
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> elems = new ArrayList<String>();
        while(rs.next()){
            elems.add(rs.getString("element_name"));
        }
        rs.close();
        return elems;
    }

    /**
     * Used to color all the extent tag locations when an 
     * annotation is loaded.
     * 
     * @return Returns a HashCollection of all the tags in the 
     * annotation; only locations where tags exists are used as 
     * keys
     * 
     * @throws Exception
     */
    HashCollection<String,String> getLocElemHash()
            throws Exception{
        HashCollection<String,String>elems = new HashCollection<String,String>();
        Statement stat = mConn.createStatement();
        String query = "select location,element_name from extents;";
        ResultSet rs = stat.executeQuery(query);
        while(rs.next()){
            elems.putEnt(rs.getString("location"), rs.getString("element_name"));
        }
        rs.close();
        return elems;
    }

    /**
     * Gets all character offsets of tags that are used to anchor the type 
     * of link selected.
     *
     * @param elem the name of the link tag being searched for
     * @return A hashtable where the keys are the character offsets 
     * of each anchor that tag type uses.
     * @throws Exception
     */
    Hashtable<Integer,String> getLocationsbyElemLink(String elem)
            throws Exception{
        Statement stat = mConn.createStatement();
        //first, get all the IDs for the extents associated with the ElemLink
        String query = "select * from links where element_name = '" + elem + "';";
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> argIds = new ArrayList<String>();
        while (rs.next()){
            for (int i=0;i<mMaxArgs;i++) {
                try {
                    String colName = "arg" + i;
                    String id = rs.getString(colName);
                    if (rs.wasNull()) {
                        break;
                    } else {
                        argIds.add(id);
                    }
                } catch (SQLException ignored) {
                    // ignore querying beyond current #args 
                    // (querying continues to maxargs)
                }
            }
        }
        rs.close();

        Hashtable<Integer,String> locs = new Hashtable<Integer,String>();
        for (String id : argIds) {
            query = "select * from extents where id = '" + id + "';";
            rs = stat.executeQuery(query);
            while (rs.next()) {
                locs.put((Integer.parseInt(rs.getString("location"))), "");
            }
        }
        return(locs);
    }

    /**
     * Used to determine what areas of the text should be bolded
     * and italicized when a link tag is unselected from the 
     * menu.
     *
     * @param elem name of the link tag being looked at
     * @param activeLinks an ArrayList of the
     * @return a hashTable of locations that should be 
     * bolded and italicized based on the selections in the
     * GUI menu
     * @throws Exception
     */
    Hashtable<Integer,String> getLocationsbyElemLink(
            String elem, ArrayList<String> activeLinks) throws Exception{
        Statement stat = mConn.createStatement();
        //first, get all the IDs for the extents associated with the ElemLink
        String query = "select * from links where element_name = '" + elem + "';";
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> argIds = new ArrayList<String>();
        while (rs.next()){
            for (int i=0;i<mMaxArgs;i++) {
                String colName = "arg"+i;
                String id = rs.getString(colName);
                if (rs.wasNull()) {
                    break;
                } else {
                    argIds.add(id);
                }
            }
        }
        rs.close();

        //then, go through and remove all the IDs that are associated with other 
        //actively bolded link tags
        for (String activated : activeLinks) {
            ArrayList<String> outIDs = new ArrayList<String>();
            query = "select * from links where element_name = '" + activated + "';";
            rs = stat.executeQuery(query);
            while (rs.next()){
                for (int i=0;i<mMaxArgs;i++) {
                    String colName = "arg"+i;
                    String id = rs.getString(colName);
                    if (rs.wasNull()) {
                        break;
                    } else {
                        outIDs.add(id);
                    }
                }
            }
            argIds.removeAll(outIDs);
        }
        //now that the list is down to only the IDs that will be removed,
        //get their locations
        Hashtable<Integer,String> locs = new Hashtable<Integer,String>();
        for (String id : argIds) {
            query = "select * from extents where id = '" + id + "';";
            rs = stat.executeQuery(query);
            while (rs.next()) {
                locs.put((Integer.parseInt(rs.getString("location"))), "");
            }
        }
        return(locs);
    }

    /**
     * Returns the start and end of an extent tag based on the ID.
     * mod by krim: multi-span support
     *
     * @param id the ID tag being searched for
     * @return a string containing the start and end locations
     * of the tag being searched for.
     * 
     * @throws Exception
     */
    ArrayList<int[]> getLocByID(String id) throws Exception{
        Statement stat = mConn.createStatement();
        String query = "select * from extents where id = '" + id + "';";
        ResultSet rs = stat.executeQuery(query);
        ArrayList<Integer>locs = new ArrayList<Integer>();
        while (rs.next()){
            locs.add(Integer.parseInt(rs.getString("location")));
        }

        Collections.sort(locs);

        rs.close();

        // add by krim: make a string representing multiple spans then return it
        int initLoc, endCandi;
        initLoc = endCandi = locs.get(0);
        ArrayList<int[]> spans = new ArrayList<int[]>();
        int[] span = new int[2];
        String s = Integer.toString(initLoc);
        span[0] = initLoc;

        if (locs.size()>1) {
            for (int loc : locs) {
                if (loc > endCandi+1) {
                    span[1] = endCandi + 1;
                    spans.add(span);
                    span[0] = loc;
                }
                endCandi = loc;
            }
        }
        span[1] = locs.get(locs.size()-1) + 1;
        spans.add(span);
        return spans;
    }

    /**
     * Return the type of an element searched by id
     * 
     * @param id the ID of the string being searched for
     * @return the tag name of the ID being searched for
     * @throws Exception
     */
    String getElemNameByID(String id)
            throws Exception{
        Statement stat = mConn.createStatement();
        // first search in extents table
        String query = "SELECT * FROM extents WHERE id = '" + id + "';";
        ResultSet rs = stat.executeQuery(query);
        String elemName;
        try {
            elemName = rs.getString("element_name");
        }
        // if search failed, try links table
        catch (SQLException e) {
            query = "SELECT * FROM links WHERE id = '" + id + "';";
            rs = stat.executeQuery(query);
            elemName =  rs.getString("element_name");
        }
        rs.close();
        return elemName;
    }

    /**
     * Removes an extent tag from the extents table
     * 
     * param element_name the name of the tag type being removed
     * @param id the ID of the tag being removed
     * @throws Exception
     */
    void removeExtentTag(String id)
            throws Exception{
        Statement stat = mConn.createStatement();
        String delete = (String.format("DELETE FROM extents WHERE id = '%s';", id));
//                +id + "'and element_name = '" + element_name+ "';");
        stat.executeUpdate(delete);  
    }

    void removeLinkTag(String id)
            throws Exception{
        Statement stat = mConn.createStatement();
        String delete = (String.format("DELETE FROM links WHERE id = '%s';", id));
//        String delete = ("DELETE FROM links WHERE id = '"
//                +id + "'and element_name = '" + element_name+ "';");
        stat.executeUpdate(delete);
    }


    /**
     * Returns the links that an extent participates in as 
     * a to or from anchor.
     * 
     * @param extType type of tag being searched for
     * @param extID ID of tag being searched for
     * @return HashCollection of tag names and IDs that are 
     * associated with the extent being searched for
     * @throws Exception
     */
    HashCollection<String,String> getLinksByExtentID(String extType, String extID)
            throws Exception{
        HashCollection<String,String> links = new HashCollection<String,String>();
        Statement stat = mConn.createStatement();
        for (int i=0; i<mMaxArgs; i++) {
            try {
                String argIdCol = "arg" + i, argTypeCol = "arg" + i + "_name";
                String query = (String.format("select id,element_name from links " +
                        "where %s = '%s' and %s  ='%s';"
                        , argIdCol, extID, argTypeCol, extType));
                ResultSet rs = stat.executeQuery(query);
                while (rs.next()) {
                    links.putEnt(rs.getString("element_name"), rs.getString("id"));
                }
                rs.close();
            } catch (SQLException ignored) {
                // ignore querying beyond current #args
                // (querying continues to maxargs)
            }
        }
        return links;
    }


    /**
     * Returns a HashCollection of ids and element types
     * that exist between the start and end character offsets.
     *
     * @param begin starting location being searched for
     * @param end ending location being searched for
     * @return HashCollection of ids and element types
     * that exist between the start and end character offsets with the
     * tag name as keys and IDs as values.
     * @throws Exception
     */
    HashCollection<String,String> getTagsInSpan(int begin, int end)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query;
        if(begin!=end){
            query = ("select distinct(id), element_name from extents where location >= "
                    + begin + " and location <=" + end + ";");
        }
        else{
            query = ("select distinct(id), element_name from extents where location = "
                    + begin + ";");
        }

        ResultSet rs = stat.executeQuery(query);
        HashCollection<String,String> tags = new HashCollection<String,String>();
        while(rs.next()){
            tags.putEnt(rs.getString("element_name"), rs.getString("id"));
        }
        rs.close();
        return tags;
    }
    
    /**
     * Returns tags in the provided span as well as all non-consuming tags
     *
     * @param begin starting location being searched for
     * @param end ending location being searched for
     * @return HashCollection of ids and element types
     * that exist between the start and end character offsets with the
     * tag name as keys and IDs as values.
     */
    HashCollection<String,String> getTagsInSpansAndNC(int begin, int end)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = "";
        if(begin!=end){
            query = ("select distinct(id), element_name from extents where location >= "
                    + begin + " and location <=" + end + ";");
        }
        else{
            query = ("select distinct(id), element_name from extents where location = "
                    + begin + ";");
        }

        ResultSet rs = stat.executeQuery(query);
        HashCollection<String,String> tags = new HashCollection<String,String>();
        while(rs.next()){
            tags.putEnt(rs.getString("element_name"),rs.getString("id"));
        }
        rs.close();

        //now get the non-consuming tags
        tags.putAll(getAllNCTags());
        return tags;
    }

    /**
     * get all extent tags in DB, return as a HashCollection
     *
     * @return HC with tag types as keys, tag ids as values
     * @throws Exception
     */
    HashCollection<String, String> getAllExtTags() throws Exception {
        Statement stat = mConn.createStatement();
        String query = "SELECT distinct(id), element_name FROM extents WHERE location != -1;";
        ResultSet rs = stat.executeQuery(query);
        HashCollection<String,String> tags = new HashCollection<String,String>();
        while(rs.next()){
            tags.putEnt(rs.getString("element_name"), rs.getString("id"));
        }
        rs.close();
        return tags;
    }

    /**
     * get all NC tags in DB, return as a HashCollection 
     * 
     * @return HC with tag types as keys, tag ids as values
     * @throws Exception
     */
    HashCollection<String,String> getAllNCTags() throws Exception {
        Statement stat = mConn.createStatement();
        String query = ("select distinct(id), element_name from extents where location = -1;");
        ResultSet rs = stat.executeQuery(query);
        HashCollection<String, String> ncTags = new HashCollection<String, String>();
        while(rs.next()){
            ncTags.putEnt(rs.getString("element_name"),rs.getString("id"));
        }
        rs.close();

        return ncTags;
    }

    /**
     * Get all ids of link tags in DB, given a name(type) of a link tag
     * * 
     * @param elemName
     * @return list of retrieved ids
     */
    ArrayList<String> getLinkIdsByName(String elemName) {
        HashSet<String> ids = new HashSet<String>();
        try {
            Statement stat = mConn.createStatement();
            String query = ("SELECT * FROM links where element_name = '" 
                    + elemName + "';");
            ResultSet rs = stat.executeQuery(query);
            while(rs.next()) {
                ids.add(rs.getString("id"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>(ids);
    }
    
    ArrayList<String> getExtIdsByName(String elemName) {
        HashSet<String> ids = new HashSet<String>();
        try {
            Statement stat = mConn.createStatement();
            String query = ("SELECT * FROM extents where element_name = '"
                    + elemName + "';");
            ResultSet rs = stat.executeQuery(query);
            while(rs.next()) {
                ids.add(rs.getString("id"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>(ids);
    }

    /**
     * Checks to see if an ID is already in use in the DB.
     * 
     * @param id ID being searched for
     * @return true or false
     * @throws Exception
     */
    boolean idExists(String id)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = "select count(id) from extents where id = '" + id + "';";
        ResultSet rs = stat.executeQuery(query);
        int num = rs.getInt(1);
        rs.close();
        if (num>0){
            return true;
        }
        // also check link table
        String query2 = "select count(id) from links where id = '" + id + "';";
        ResultSet rs2 = stat.executeQuery(query2);
        int num2 = rs2.getInt(1);
        rs2.close();
        return num2 > 0;

    }


    /**
     * Adds a single extent to the batch command
     * 
     * @param location character offset
     * @param element tag name
     * @param id ID
     * @throws Exception
     */
    void addExtent(int location, String element, String id)
            throws Exception{
        mExt2Insert.setInt(LOC_COL, location);
        mExt2Insert.setString(NAME_COL, element);
        mExt2Insert.setString(ID_COL, id);
        mExt2Insert.addBatch();
    }

    /**
     * Adds a set of extents to the DB at once
     * 
     * @throws Exception
     */
    void batchExtents() throws Exception{
        mConn.setAutoCommit(false);
        mExt2Insert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Adds a single extent to the DB
     * @param location character offset
     * @param element tag name
     * @param id ID
     * @throws Exception
     */
    void insertExtent(int location, String element, String id)
            throws Exception{
        addExtent(location, element, id);
        batchExtents();
    }

    /**
     * Adds a set of links to the DB at once
     * @throws Exception
     */
    void batchLinks() throws Exception{
        mConn.setAutoCommit(false);
        mLink2Insert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Adds a link to the batch
     * @param id ID string for a new link
     * @param name type of link being added
     * @param argIds list of ids of relevent arguments
     * @param argTypes list of names of relevent arguments (should correspond to args)
     */
    void addLink(String id, String name,
                 List<String> argIds, List<String> argTypes) {
        // first check args and argTypes are matching
        // (maybe checking here is redundant, since we can't give any message to a user)
        if (argIds.size() != argTypes.size()) {
            System.err.println("args and argTypes not matching");
        }
        // or the number of arguments is acceptable
        else if (argIds.size() > mMaxArgs) {
            System.err.println(argIds.toString() + "CUR_MAX: " + mMaxArgs);
            System.err.println("too many arguments");
        }
        // else, that is, input seems to be good enough
        else {
            try {
                mLink2Insert.setString(ID_COL, id);
                mLink2Insert.setString(NAME_COL, name);
                for (int i=0;i<argIds.size();i++) {
                    mLink2Insert.setString(ARG0_COL + (2*i), argIds.get(i));
                    mLink2Insert.setString(ARG0_COL + (2*i)+1, argTypes.get(i));
                }
                mLink2Insert.addBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Update a link tag with a single specific argument of it
     * @param id
     * @param argNum
     * @param argId
     * @param argType
     * @throws SQLException
     */
    void addArgument(String id, int argNum,
                     String argId, String argType) throws SQLException {
        Statement stat = mConn.createStatement();
        String argIdCol = "arg" + argNum, argTypeCol = "arg" + argNum + "_name";
        String update
                = String.format(
                "UPDATE links SET %s = '%s', %s = '%s' where id = '%s';", 
                argIdCol, argId, argTypeCol, argType, id);
                
        stat.executeUpdate(update);
        
    }

    /**
     * Closes the connection to the DB
     */
    void closeDb(){
        try{
            mConn.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * getter for max # arguments
     * @return largest number among numbers of arguments in all link tags in DB
     */
    int getMaxArgs() {
        return mMaxArgs;
    }
    
}


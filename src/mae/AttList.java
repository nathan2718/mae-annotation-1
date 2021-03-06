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

/**
 * Used for tag attributes that provide a list of options
 * @author Amber Stubbs, Keigh Rim
 *
 */


import java.util.ArrayList;

class AttList extends Attrib{

    AttList(){
    }


    AttList (String name, boolean required, 
             ArrayList<String> validList, String defaultValue){
        setName(name);
        setRequired(required);
        setVaildValues(validList);
        setDefaultValue(defaultValue);
    }

    public ArrayList<String> getVaildValues(){
        return vaildValues;
    }

    public void setVaildValues(ArrayList<String> vaildList){
        vaildValues =vaildList;
    }

    public String toString(){
        return("Attribute name =" + getName() + " , required = " + isRequired() + "also list" );
    }


    private ArrayList<String> vaildValues;

}
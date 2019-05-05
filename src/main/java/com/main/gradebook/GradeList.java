/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.main.gradebook;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 *
 * @author rasheed
 */

@XmlRootElement
@XmlSeeAlso({Gradebook.class})
public class GradeList<T> {
    
    private List<T> listOfObjectEntity;
    
    public GradeList() {
        listOfObjectEntity = new ArrayList<T>();
    }
    
    public GradeList(List<T> entityList) {
        this.listOfObjectEntity = entityList;
    }
    
    @XmlAnyElement
    public List<T> getItems() {
        return listOfObjectEntity;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.main.gradebook;

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
@XmlSeeAlso({Student.class})
public class StudentList<T> {
    
    private List<T> listOfObjectEntity;
    
    public StudentList() {
        listOfObjectEntity = new ArrayList<T>();
    }
    
    public StudentList(List<T> entityList) {
        this.listOfObjectEntity = entityList;
    }
    
    @XmlAnyElement
    public List<T> getItems() {
        return listOfObjectEntity;
    }  
}

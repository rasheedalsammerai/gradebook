/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.main.gradebook;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * 
 */
@XmlRootElement(name = "student")
public class Student {
    
    private String name;
    private int id;
    private String grade;
   
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }
   
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.main.gradebook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author rasheed
 */
@XmlRootElement(name = "gradebooklist")
public class GradeBookList implements Serializable {
    
    private List<Gradebook> gradebook = new ArrayList<>();
    
    public List<Gradebook> getGrades () {
        return gradebook;
    }
    
    public void setGradebook(List<Gradebook> gradebook) {
        this.gradebook = gradebook;
    }
}

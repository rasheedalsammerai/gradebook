/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.main.gradebook;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author rasheed
 */
@XmlRootElement(name = "port")
public class Port {
    String port;

    public  int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    int id;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
    
    
}

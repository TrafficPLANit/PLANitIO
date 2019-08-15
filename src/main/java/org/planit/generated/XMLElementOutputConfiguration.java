//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.15 at 02:23:24 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for outputconfiguration element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="outputconfiguration"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}assignment"/&gt;
 *           &lt;element ref="{}physicalcost"/&gt;
 *           &lt;element ref="{}virtualcost"/&gt;
 *           &lt;element ref="{}timeperiod"/&gt;
 *         &lt;/sequence&gt;
 *       &lt;/restriction&gt;
 *     &lt;/complexContent&gt;
 *   &lt;/complexType&gt;
 * &lt;/element&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "assignment",
    "physicalcost",
    "virtualcost",
    "timeperiod"
})
@XmlRootElement(name = "outputconfiguration")
public class XMLElementOutputConfiguration {

    @XmlElement(required = true)
    protected String assignment;
    @XmlElement(required = true)
    protected String physicalcost;
    @XmlElement(required = true)
    protected String virtualcost;
    @XmlElement(required = true)
    protected XMLElementOutputTimePeriod timeperiod;

    /**
     * Gets the value of the assignment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAssignment() {
        return assignment;
    }

    /**
     * Sets the value of the assignment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAssignment(String value) {
        this.assignment = value;
    }

    /**
     * Gets the value of the physicalcost property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhysicalcost() {
        return physicalcost;
    }

    /**
     * Sets the value of the physicalcost property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhysicalcost(String value) {
        this.physicalcost = value;
    }

    /**
     * Gets the value of the virtualcost property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVirtualcost() {
        return virtualcost;
    }

    /**
     * Sets the value of the virtualcost property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVirtualcost(String value) {
        this.virtualcost = value;
    }

    /**
     * Gets the value of the timeperiod property.
     * 
     * @return
     *     possible object is
     *     {@link XMLElementOutputTimePeriod }
     *     
     */
    public XMLElementOutputTimePeriod getTimeperiod() {
        return timeperiod;
    }

    /**
     * Sets the value of the timeperiod property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementOutputTimePeriod }
     *     
     */
    public void setTimeperiod(XMLElementOutputTimePeriod value) {
        this.timeperiod = value;
    }

}

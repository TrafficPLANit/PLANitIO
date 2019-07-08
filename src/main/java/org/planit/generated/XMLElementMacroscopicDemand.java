//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.08 at 09:34:23 AM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				The macroscopic demand, i.e., in traffic flows not discrete vehicles, for the project at hand per mode per time period, per od 
 * 			
 * 
 * <p>Java class for macroscopicdemand element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="macroscopicdemand"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}demandconfiguration" minOccurs="0"/&gt;
 *           &lt;element ref="{}oddemands"/&gt;
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
    "demandconfiguration",
    "oddemands"
})
@XmlRootElement(name = "macroscopicdemand")
public class XMLElementMacroscopicDemand {

    protected XMLElementDemandConfiguration demandconfiguration;
    @XmlElement(required = true)
    protected XMLElementOdDemands oddemands;

    /**
     * 
     * 							In case the configuration is absent it must have been part of another demand input file that has already been parsed
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementDemandConfiguration }
     *     
     */
    public XMLElementDemandConfiguration getDemandconfiguration() {
        return demandconfiguration;
    }

    /**
     * Sets the value of the demandconfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementDemandConfiguration }
     *     
     */
    public void setDemandconfiguration(XMLElementDemandConfiguration value) {
        this.demandconfiguration = value;
    }

    /**
     * Gets the value of the oddemands property.
     * 
     * @return
     *     possible object is
     *     {@link XMLElementOdDemands }
     *     
     */
    public XMLElementOdDemands getOddemands() {
        return oddemands;
    }

    /**
     * Sets the value of the oddemands property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementOdDemands }
     *     
     */
    public void setOddemands(XMLElementOdDemands value) {
        this.oddemands = value;
    }

}

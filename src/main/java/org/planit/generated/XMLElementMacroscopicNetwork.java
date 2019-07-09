//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.09 at 11:03:41 AM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Root element of the macroscopic network infrastructure. Defines the physical road network and its properties
 * 			
 * 
 * <p>Java class for macroscopicnetwork element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="macroscopicnetwork"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}linkconfiguration" minOccurs="0"/&gt;
 *           &lt;element ref="{}infrastructure"/&gt;
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
    "linkconfiguration",
    "infrastructure"
})
@XmlRootElement(name = "macroscopicnetwork")
public class XMLElementMacroscopicNetwork {

    protected XMLElementLinkConfiguration linkconfiguration;
    @XmlElement(required = true)
    protected XMLElementInfrastructure infrastructure;

    /**
     * 
     * 							When configuration is absent we assume a single link segment type for the entire simulation and a simple Cartesian coordinate system with top-left (0,0) 
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementLinkConfiguration }
     *     
     */
    public XMLElementLinkConfiguration getLinkconfiguration() {
        return linkconfiguration;
    }

    /**
     * Sets the value of the linkconfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementLinkConfiguration }
     *     
     */
    public void setLinkconfiguration(XMLElementLinkConfiguration value) {
        this.linkconfiguration = value;
    }

    /**
     * Gets the value of the infrastructure property.
     * 
     * @return
     *     possible object is
     *     {@link XMLElementInfrastructure }
     *     
     */
    public XMLElementInfrastructure getInfrastructure() {
        return infrastructure;
    }

    /**
     * Sets the value of the infrastructure property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementInfrastructure }
     *     
     */
    public void setInfrastructure(XMLElementInfrastructure value) {
        this.infrastructure = value;
    }

}
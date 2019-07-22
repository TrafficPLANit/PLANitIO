//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.22 at 05:37:36 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Configuration, i.e., meta data of the network infrastructure 
 * 			
 * 
 * <p>Java class for linkconfiguration element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="linkconfiguration"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}modes" minOccurs="0"/&gt;
 *           &lt;element ref="{}linksegmenttypes" minOccurs="0"/&gt;
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
    "modes",
    "linksegmenttypes"
})
@XmlRootElement(name = "linkconfiguration")
public class XMLElementLinkConfiguration {

    protected XMLElementModes modes;
    protected XMLElementLinkSegmentTypes linksegmenttypes;

    /**
     * 
     * 							Modes available in this project. Note that when this element is absent a single 'virtual' mode is created to represent 
     * 							all modes. If this is the case, no references to modes are to be made in any forthcoming elements as there is nothing to refer
     * 							to, i.e., no ids of modes are present so they cannot be referred.
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementModes }
     *     
     */
    public XMLElementModes getModes() {
        return modes;
    }

    /**
     * Sets the value of the modes property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementModes }
     *     
     */
    public void setModes(XMLElementModes value) {
        this.modes = value;
    }

    /**
     * 
     * 							Link segment types define the different types of link segment properties that can be attributed to links. Note
     * 							that it is allowed to not define this element. In that case all link(segments) are assumed to be of the same default
     * 							link segment type. 
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementLinkSegmentTypes }
     *     
     */
    public XMLElementLinkSegmentTypes getLinksegmenttypes() {
        return linksegmenttypes;
    }

    /**
     * Sets the value of the linksegmenttypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementLinkSegmentTypes }
     *     
     */
    public void setLinksegmenttypes(XMLElementLinkSegmentTypes value) {
        this.linksegmenttypes = value;
    }

}

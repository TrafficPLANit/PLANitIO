//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.23 at 01:57:07 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import net.opengis.gml.PointType;


/**
 * 
 * 				Centroid is a point of departure and/or arrival of travellers from/to a zone. 
 * 			
 * 
 * <p>Java class for centroid element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="centroid"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *           &lt;element ref="{http://www.opengis.net/gml}Point"/&gt;
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
    "name",
    "point"
})
@XmlRootElement(name = "centroid")
public class XMLElementCentroid {

    @XmlElement(defaultValue = "")
    protected String name;
    @XmlElement(name = "Point", namespace = "http://www.opengis.net/gml", required = true)
    protected PointType point;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * 
     * 							Each centroids geo spatial location determines its location (point of departure/arrival). It should fall within the zones
     * 							geo spatial area (if defined). 
     * 						
     * 
     * @return
     *     possible object is
     *     {@link PointType }
     *     
     */
    public PointType getPoint() {
        return point;
    }

    /**
     * Sets the value of the point property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointType }
     *     
     */
    public void setPoint(PointType value) {
        this.point = value;
    }

}

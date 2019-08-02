//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.02 at 02:11:34 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				The physical infrastructure consists of nodes and links 
 * 			
 * 
 * <p>Java class for infrastructure element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="infrastructure"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}nodes"/&gt;
 *           &lt;element ref="{}links"/&gt;
 *         &lt;/sequence&gt;
 *         &lt;attribute ref="{}srsname"/&gt;
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
    "nodes",
    "links"
})
@XmlRootElement(name = "infrastructure")
public class XMLElementInfrastructure {

    @XmlElement(required = true)
    protected XMLElementNodes nodes;
    @XmlElement(required = true)
    protected XMLElementLinks links;
    @XmlAttribute(name = "srsname")
    protected Srsname srsname;

    /**
     * Gets the value of the nodes property.
     * 
     * @return
     *     possible object is
     *     {@link XMLElementNodes }
     *     
     */
    public XMLElementNodes getNodes() {
        return nodes;
    }

    /**
     * Sets the value of the nodes property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementNodes }
     *     
     */
    public void setNodes(XMLElementNodes value) {
        this.nodes = value;
    }

    /**
     * Gets the value of the links property.
     * 
     * @return
     *     possible object is
     *     {@link XMLElementLinks }
     *     
     */
    public XMLElementLinks getLinks() {
        return links;
    }

    /**
     * Sets the value of the links property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementLinks }
     *     
     */
    public void setLinks(XMLElementLinks value) {
        this.links = value;
    }

    /**
     * 
     * 						Spatial reference system chosen for infrastructure, if absent Cartesian system is assume with 0,0 to be the top left 
     * 					
     * 
     * @return
     *     possible object is
     *     {@link Srsname }
     *     
     */
    public Srsname getSrsname() {
        return srsname;
    }

    /**
     * Sets the value of the srsname property.
     * 
     * @param value
     *     allowed object is
     *     {@link Srsname }
     *     
     */
    public void setSrsname(Srsname value) {
        this.srsname = value;
    }

}

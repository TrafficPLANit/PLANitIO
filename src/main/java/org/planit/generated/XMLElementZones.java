//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.22 at 02:41:43 PM AEST 
//


package org.planit.generated;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import net.opengis.gml.PolygonType;


/**
 * 
 * 				Zones are the geographical areas for which we construct travel demand which are loaded onto the network at a single point (Centroid)
 * 			
 * 
 * <p>Java class for zones element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="zones"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element name="zone" maxOccurs="unbounded"&gt;
 *             &lt;complexType&gt;
 *               &lt;complexContent&gt;
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                   &lt;sequence&gt;
 *                     &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                     &lt;element ref="{}centroid"/&gt;
 *                     &lt;element ref="{}connectoids"/&gt;
 *                     &lt;element ref="{http://www.opengis.net/gml}Polygon" minOccurs="0"/&gt;
 *                   &lt;/sequence&gt;
 *                   &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" /&gt;
 *                 &lt;/restriction&gt;
 *               &lt;/complexContent&gt;
 *             &lt;/complexType&gt;
 *           &lt;/element&gt;
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
    "zone"
})
@XmlRootElement(name = "zones")
public class XMLElementZones {

    @XmlElement(required = true)
    protected List<XMLElementZones.Zone> zone;
    @XmlAttribute(name = "srsname")
    protected Srsname srsname;

    /**
     * Gets the value of the zone property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the zone property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getZone().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XMLElementZones.Zone }
     * 
     * 
     */
    public List<XMLElementZones.Zone> getZone() {
        if (zone == null) {
            zone = new ArrayList<XMLElementZones.Zone>();
        }
        return this.zone;
    }

    /**
     * 
     * 						Spatial reference system is optional, when absent it is assumed that the same srs is used as provided on the infrastrure element in the
     * 						macroscopic supply input. If present it can be different but it is recommended to be the same. When different the gml elements in this xml
     * 						are parsed according to the provided srs but will internally be converted into the "dominant" project srs which is the one provided on the infrastructure element
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


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element ref="{}centroid"/&gt;
     *         &lt;element ref="{}connectoids"/&gt;
     *         &lt;element ref="{http://www.opengis.net/gml}Polygon" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" /&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "centroid",
        "connectoids",
        "polygon"
    })
    public static class Zone {

        @XmlElement(defaultValue = "")
        protected String name;
        @XmlElement(required = true)
        protected XMLElementCentroid centroid;
        @XmlElement(required = true)
        protected XMLElementConnectoids connectoids;
        @XmlElement(name = "Polygon", namespace = "http://www.opengis.net/gml")
        protected PolygonType polygon;
        @XmlAttribute(name = "id", required = true)
        @XmlSchemaType(name = "positiveInteger")
        protected BigInteger id;

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
         * 										Each travel zone has at most a single centroid from which all travellers depart/arrive. 
         * 									
         * 
         * @return
         *     possible object is
         *     {@link XMLElementCentroid }
         *     
         */
        public XMLElementCentroid getCentroid() {
            return centroid;
        }

        /**
         * Sets the value of the centroid property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLElementCentroid }
         *     
         */
        public void setCentroid(XMLElementCentroid value) {
            this.centroid = value;
        }

        /**
         * Gets the value of the connectoids property.
         * 
         * @return
         *     possible object is
         *     {@link XMLElementConnectoids }
         *     
         */
        public XMLElementConnectoids getConnectoids() {
            return connectoids;
        }

        /**
         * Sets the value of the connectoids property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLElementConnectoids }
         *     
         */
        public void setConnectoids(XMLElementConnectoids value) {
            this.connectoids = value;
        }

        /**
         * 
         * 										Each travel zone has a geographical area in the form of a Polygon. We recommend not using an interior polygon as "islands"
         * 										are generally not allowed in traffic assignment. Also zones should not overlap and instead be contiguous (reusing borders). 
         * 									
         * 
         * @return
         *     possible object is
         *     {@link PolygonType }
         *     
         */
        public PolygonType getPolygon() {
            return polygon;
        }

        /**
         * Sets the value of the polygon property.
         * 
         * @param value
         *     allowed object is
         *     {@link PolygonType }
         *     
         */
        public void setPolygon(PolygonType value) {
            this.polygon = value;
        }

        /**
         * Gets the value of the id property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getId() {
            return id;
        }

        /**
         * Sets the value of the id property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setId(BigInteger value) {
            this.id = value;
        }

    }

}

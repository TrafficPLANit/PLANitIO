//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.05 at 12:38:04 PM AEST 
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


/**
 * 
 * 				The link segment types can be used to group similar link characteristics in one place. Each link segment has exactly one 
 * 				link segment type associated with it
 * 			
 * 
 * <p>Java class for linksegmenttypes element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="linksegmenttypes"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element name="linksegmenttype" maxOccurs="unbounded"&gt;
 *             &lt;complexType&gt;
 *               &lt;complexContent&gt;
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                   &lt;sequence&gt;
 *                     &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *                     &lt;element ref="{}maxdensitylane" minOccurs="0"/&gt;
 *                     &lt;element ref="{}capacitylane" minOccurs="0"/&gt;
 *                     &lt;element name="modes" minOccurs="0"&gt;
 *                       &lt;complexType&gt;
 *                         &lt;complexContent&gt;
 *                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                             &lt;sequence&gt;
 *                               &lt;element name="mode" maxOccurs="unbounded"&gt;
 *                                 &lt;complexType&gt;
 *                                   &lt;complexContent&gt;
 *                                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                       &lt;sequence&gt;
 *                                         &lt;element ref="{}maxspeed" minOccurs="0"/&gt;
 *                                         &lt;element ref="{}critspeed" minOccurs="0"/&gt;
 *                                       &lt;/sequence&gt;
 *                                       &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
 *                                     &lt;/restriction&gt;
 *                                   &lt;/complexContent&gt;
 *                                 &lt;/complexType&gt;
 *                               &lt;/element&gt;
 *                             &lt;/sequence&gt;
 *                           &lt;/restriction&gt;
 *                         &lt;/complexContent&gt;
 *                       &lt;/complexType&gt;
 *                     &lt;/element&gt;
 *                   &lt;/sequence&gt;
 *                   &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" /&gt;
 *                 &lt;/restriction&gt;
 *               &lt;/complexContent&gt;
 *             &lt;/complexType&gt;
 *           &lt;/element&gt;
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
    "linksegmenttype"
})
@XmlRootElement(name = "linksegmenttypes")
public class XMLElementLinkSegmentTypes {

    @XmlElement(required = true)
    protected List<XMLElementLinkSegmentTypes.Linksegmenttype> linksegmenttype;

    /**
     * Gets the value of the linksegmenttype property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linksegmenttype property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinksegmenttype().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XMLElementLinkSegmentTypes.Linksegmenttype }
     * 
     * 
     */
    public List<XMLElementLinkSegmentTypes.Linksegmenttype> getLinksegmenttype() {
        if (linksegmenttype == null) {
            linksegmenttype = new ArrayList<XMLElementLinkSegmentTypes.Linksegmenttype>();
        }
        return this.linksegmenttype;
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
     *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
     *         &lt;element ref="{}maxdensitylane" minOccurs="0"/&gt;
     *         &lt;element ref="{}capacitylane" minOccurs="0"/&gt;
     *         &lt;element name="modes" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="mode" maxOccurs="unbounded"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element ref="{}maxspeed" minOccurs="0"/&gt;
     *                             &lt;element ref="{}critspeed" minOccurs="0"/&gt;
     *                           &lt;/sequence&gt;
     *                           &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" /&gt;
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
        "maxdensitylane",
        "capacitylane",
        "modes"
    })
    public static class Linksegmenttype {

        @XmlElement(required = true, defaultValue = "")
        protected String name;
        @XmlElement(defaultValue = "180.0")
        protected Float maxdensitylane;
        @XmlElement(defaultValue = "1800.0")
        protected Float capacitylane;
        protected XMLElementLinkSegmentTypes.Linksegmenttype.Modes modes;
        @XmlAttribute(name = "id")
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
         * 										When not present, the default maximum density per lane will be applied
         * 									
         * 
         * @return
         *     possible object is
         *     {@link Float }
         *     
         */
        public Float getMaxdensitylane() {
            return maxdensitylane;
        }

        /**
         * Sets the value of the maxdensitylane property.
         * 
         * @param value
         *     allowed object is
         *     {@link Float }
         *     
         */
        public void setMaxdensitylane(Float value) {
            this.maxdensitylane = value;
        }

        /**
         * 
         * 										When not present, the default capacity per lane will be applied
         * 									
         * 
         * @return
         *     possible object is
         *     {@link Float }
         *     
         */
        public Float getCapacitylane() {
            return capacitylane;
        }

        /**
         * Sets the value of the capacitylane property.
         * 
         * @param value
         *     allowed object is
         *     {@link Float }
         *     
         */
        public void setCapacitylane(Float value) {
            this.capacitylane = value;
        }

        /**
         * Gets the value of the modes property.
         * 
         * @return
         *     possible object is
         *     {@link XMLElementLinkSegmentTypes.Linksegmenttype.Modes }
         *     
         */
        public XMLElementLinkSegmentTypes.Linksegmenttype.Modes getModes() {
            return modes;
        }

        /**
         * Sets the value of the modes property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLElementLinkSegmentTypes.Linksegmenttype.Modes }
         *     
         */
        public void setModes(XMLElementLinkSegmentTypes.Linksegmenttype.Modes value) {
            this.modes = value;
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
         *         &lt;element name="mode" maxOccurs="unbounded"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element ref="{}maxspeed" minOccurs="0"/&gt;
         *                   &lt;element ref="{}critspeed" minOccurs="0"/&gt;
         *                 &lt;/sequence&gt;
         *                 &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/sequence&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "mode"
        })
        public static class Modes {

            @XmlElement(required = true)
            protected List<XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode> mode;

            /**
             * Gets the value of the mode property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the mode property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getMode().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode }
             * 
             * 
             */
            public List<XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode> getMode() {
                if (mode == null) {
                    mode = new ArrayList<XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode>();
                }
                return this.mode;
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
             *         &lt;element ref="{}maxspeed" minOccurs="0"/&gt;
             *         &lt;element ref="{}critspeed" minOccurs="0"/&gt;
             *       &lt;/sequence&gt;
             *       &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "maxspeed",
                "critspeed"
            })
            public static class Mode {

                protected Float maxspeed;
                protected Float critspeed;
                @XmlAttribute(name = "ref")
                @XmlSchemaType(name = "nonNegativeInteger")
                protected BigInteger ref;

                /**
                 * Gets the value of the maxspeed property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Float }
                 *     
                 */
                public Float getMaxspeed() {
                    return maxspeed;
                }

                /**
                 * Sets the value of the maxspeed property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Float }
                 *     
                 */
                public void setMaxspeed(Float value) {
                    this.maxspeed = value;
                }

                /**
                 * Gets the value of the critspeed property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Float }
                 *     
                 */
                public Float getCritspeed() {
                    return critspeed;
                }

                /**
                 * Sets the value of the critspeed property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Float }
                 *     
                 */
                public void setCritspeed(Float value) {
                    this.critspeed = value;
                }

                /**
                 * Gets the value of the ref property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link BigInteger }
                 *     
                 */
                public BigInteger getRef() {
                    return ref;
                }

                /**
                 * Sets the value of the ref property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link BigInteger }
                 *     
                 */
                public void setRef(BigInteger value) {
                    this.ref = value;
                }

            }

        }

    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.16 at 04:13:23 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for demandconfiguration element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="demandconfiguration"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}travellertypes" minOccurs="0"/&gt;
 *           &lt;element ref="{}userclasses" minOccurs="0"/&gt;
 *           &lt;element ref="{}timeperiods"/&gt;
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
    "travellertypes",
    "userclasses",
    "timeperiods"
})
@XmlRootElement(name = "demandconfiguration")
public class XMLElementDemandConfiguration {

    protected XMLElementTravellerTypes travellertypes;
    protected XMLElementUserClasses userclasses;
    @XmlElement(required = true)
    protected XMLElementTimePeriods timeperiods;

    /**
     * 
     * 							Traveller types available in this project. Note that when this element is absent a single 'virtual' traveller type is created to represent 
     * 							all travellers. If this is the case, no references to traveller types are to be made in any forthcoming elements as there is nothing to refer
     * 							to, i.e., no ids of traveller types are present so they cannot be referred.
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementTravellerTypes }
     *     
     */
    public XMLElementTravellerTypes getTravellertypes() {
        return travellertypes;
    }

    /**
     * Sets the value of the travellertypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementTravellerTypes }
     *     
     */
    public void setTravellertypes(XMLElementTravellerTypes value) {
        this.travellertypes = value;
    }

    /**
     * 
     * 							Userclasses define the types of travellers we have by combining a mode with a traveller type. In case no userclasses are explicitly defined, i.e.,
     * 							the element is absent, then it is assumed we only have a single 'virtual' user class. In that case at most a single mode and/or traveller type can be defined
     * 							otherwise the user class is no longer uniquely defined. Similarly, no references to userclasses can be made anywhere and instead the virtual user class is assumed
     * 							automatically.
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementUserClasses }
     *     
     */
    public XMLElementUserClasses getUserclasses() {
        return userclasses;
    }

    /**
     * Sets the value of the userclasses property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementUserClasses }
     *     
     */
    public void setUserclasses(XMLElementUserClasses value) {
        this.userclasses = value;
    }

    /**
     * 
     * 							Time periods determine how the travel demand in the origin-destination matrices is to be interpreted. As many time periods as one wishes can be defined and each
     * 							od-matrix can be related to at most a single time period.
     * 						
     * 
     * @return
     *     possible object is
     *     {@link XMLElementTimePeriods }
     *     
     */
    public XMLElementTimePeriods getTimeperiods() {
        return timeperiods;
    }

    /**
     * Sets the value of the timeperiods property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLElementTimePeriods }
     *     
     */
    public void setTimeperiods(XMLElementTimePeriods value) {
        this.timeperiods = value;
    }

}

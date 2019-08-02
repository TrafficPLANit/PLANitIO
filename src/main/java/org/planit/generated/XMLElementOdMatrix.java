//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.02 at 02:11:34 PM AEST 
//


package org.planit.generated;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for odmatrix complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="odmatrix"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute ref="{}timeperiodref use="required""/&gt;
 *       &lt;attribute ref="{}userclassref"/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "odmatrix")
@XmlSeeAlso({
    XMLElementOdCellByCellMatrix.class,
    XMLElementOdRawMatrix.class,
    XMLElementOdRowMatrix.class
})
public class XMLElementOdMatrix {

    @XmlAttribute(name = "timeperiodref", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger timeperiodref;
    @XmlAttribute(name = "userclassref")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger userclassref;

    /**
     * Gets the value of the timeperiodref property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTimeperiodref() {
        return timeperiodref;
    }

    /**
     * Sets the value of the timeperiodref property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTimeperiodref(BigInteger value) {
        this.timeperiodref = value;
    }

    /**
     * Gets the value of the userclassref property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getUserclassref() {
        return userclassref;
    }

    /**
     * Sets the value of the userclassref property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setUserclassref(BigInteger value) {
        this.userclassref = value;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.10 at 05:32:42 PM AEST 
//


package org.planit.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Origin destination travel demands can be provided in various formats depending on what the user prefers. Either very readable but bloated (in terms of space)
 * 				or more efficient but less human readable. Multiple matrices can be defined here, or they can be spread across multiple files if this is preferred in terms
 * 				of file size
 * 			
 * 
 * <p>Java class for oddemands element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="oddemands"&gt;
 *   &lt;complexType&gt;
 *     &lt;complexContent&gt;
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *         &lt;choice maxOccurs="unbounded"&gt;
 *           &lt;element ref="{}odcellbycellmatrix"/&gt;
 *           &lt;element ref="{}odrowmatrix"/&gt;
 *           &lt;element ref="{}odrawmatrix"/&gt;
 *         &lt;/choice&gt;
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
    "odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix"
})
@XmlRootElement(name = "oddemands")
public class XMLElementOdDemands {

    @XmlElements({
        @XmlElement(name = "odcellbycellmatrix", type = XMLElementOdCellByCellMatrix.class),
        @XmlElement(name = "odrowmatrix", type = XMLElementOdRowMatrix.class),
        @XmlElement(name = "odrawmatrix", type = XMLElementOdRawMatrix.class)
    })
    protected List<XMLElementOdMatrix> odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix;

    /**
     * Gets the value of the odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XMLElementOdCellByCellMatrix }
     * {@link XMLElementOdRowMatrix }
     * {@link XMLElementOdRawMatrix }
     * 
     * 
     */
    public List<XMLElementOdMatrix> getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix() {
        if (odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix == null) {
            odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix = new ArrayList<XMLElementOdMatrix>();
        }
        return this.odcellbycellmatrixOrOdrowmatrixOrOdrawmatrix;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.19 at 06:24:31 PM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Duration is a positive integer (in seconds) and we start counting from the start time. Note that the duration must always be less
 * 				than the number of seconds in a day (86400). 
 * 			
 * 
 * <p>Java class for duration element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="duration"&gt;
 *   &lt;complexType&gt;
 *     &lt;simpleContent&gt;
 *       &lt;restriction base="&lt;&gt;unrestrictedduration"&gt;
 *       &lt;/restriction&gt;
 *     &lt;/simpleContent&gt;
 *   &lt;/complexType&gt;
 * &lt;/element&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "duration")
public class XMLElementDuration
    extends XMLElementUnrestrictedDuration
{


}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.01 at 10:10:48 AM AEST 
//


package org.planit.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for typevalues.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="typevalues"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="double"/&gt;
 *     &lt;enumeration value="float"/&gt;
 *     &lt;enumeration value="integer"/&gt;
 *     &lt;enumeration value="boolean"/&gt;
 *     &lt;enumeration value="srsname"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "typevalues")
@XmlEnum
public enum Typevalues {

    @XmlEnumValue("double")
    DOUBLE("double"),
    @XmlEnumValue("float")
    FLOAT("float"),
    @XmlEnumValue("integer")
    INTEGER("integer"),
    @XmlEnumValue("boolean")
    BOOLEAN("boolean"),
    @XmlEnumValue("srsname")
    SRSNAME("srsname");
    private final String value;

    Typevalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Typevalues fromValue(String v) {
        for (Typevalues c: Typevalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

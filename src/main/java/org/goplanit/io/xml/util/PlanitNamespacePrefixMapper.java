package org.goplanit.io.xml.util;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class PlanitNamespacePrefixMapper extends NamespacePrefixMapper {
  
  public static String NAMESPACE_OPENGIS_URI = "http://www.opengis.net/gml";
    
  public static String NAMESPACE_OPENGIS_PREFIX = "gml";

  /**
   * provide alternative prefix for namespace
   */
  @Override
  public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
    if(namespaceUri.equals(NAMESPACE_OPENGIS_URI)) {
      return NAMESPACE_OPENGIS_PREFIX;
    }else {
      return suggestion;
    }
  }

}

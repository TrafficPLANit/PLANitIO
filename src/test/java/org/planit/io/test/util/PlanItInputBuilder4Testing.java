package org.planit.io.test.util;

import java.util.logging.Logger;

import org.planit.io.input.PlanItInputBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.time.TimePeriod;

/**
 * Class which reads inputs from XML input files
 *
 * @author gman6028
 *
 */
public class PlanItInputBuilder4Testing extends PlanItInputBuilder {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanItInputBuilder4Testing.class.getCanonicalName());
  

  /**
   * Constructor which generates the input objects from files in a specified
   * directory, using the default extension ".xml"
   *
   * @param projectPath the location of the input file directory
   * @throws PlanItException thrown if one of the input required input files cannot be found, or if there is an error reading one of them
   */
  public PlanItInputBuilder4Testing(final String projectPath) throws PlanItException {
    super(projectPath);
  }

  /**
   * Constructor which generates the input objects from files in a specified
   * directory
   *
   * @param projectPath the location of the input file directory
   * @param xmlFileExtension the extension of the data files to be searched through
   * @throws PlanItException thrown if one of the input required input files cannot be found, or if there is an error reading one of them
   */
  public PlanItInputBuilder4Testing(final String projectPath, final String xmlFileExtension) throws PlanItException {
    super(projectPath, xmlFileExtension);    
  }

   
  
  /**
   * returns the link segment by a given XML id, only accessible directly for testing purposes
   * 
   * @param network    to look in
   * @param XmlId to look for
   * @return link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByXmlId(String XmlId) {
    return this.getPlanitNetworkReader().getLinkSegmentBySourceId(XmlId);
  } 
  
  /**
   * returns the link segment type by a given XML id, only accessible directly for testing purposes
   * 
   * @param XmlId to look for
   * @return link segment type found
   */
  public MacroscopicLinkSegmentType getLinkSegmentTypeByXmlId(String XmlId) {
    return this.getPlanitNetworkReader().getLinkSegmentTypeBySourceId(XmlId);
  }

  /**
   * returns the mode by a given XML id, only accessible directly for testing purposes
   * 
   * @param XmlId to look for
   * @return mode found
   */  
  public Mode getModeByXmlId(String XmlId) {
    return this.getPlanitNetworkReader().getModeBySourceId(XmlId);
  }

  /**
   * returns the time period by a given XML id, only accessible directly for testing purposes
   * 
   * @param XmlId to look for
   * @return time period found
   */    
  public TimePeriod getTimePeriodByXmlId(String xmlId) {
    return this.getPlanitDemandsReader().getTimePeriodBySourceId(xmlId);
  }

  /**
   * returns the node by a given XML id, only accessible directly for testing purposes
   * 
   * @param XmlId to look for
   * @return node found
   */      
  public Node getNodeByXmlId(String nodeXmlId) {
    return getPlanitNetworkReader().getNodeBySourceId(nodeXmlId);
  }  

}
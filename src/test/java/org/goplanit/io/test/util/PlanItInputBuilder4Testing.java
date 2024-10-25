package org.goplanit.io.test.util;

import java.util.logging.Logger;

import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Class which reads inputs from XML input files
 *
 * @author gman6028
 *
 */
@Deprecated
public class PlanItInputBuilder4Testing extends PlanItInputBuilder {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanItInputBuilder4Testing.class.getCanonicalName());
  

  /**
   * Constructor which generates the input objects from files in a specified
   * directory, using the default extension ".xml"
   *
   * @param projectPath the location of the input file directory
   */
  public PlanItInputBuilder4Testing(final String projectPath){
    super(projectPath);
  }

}
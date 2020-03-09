package org.planit.planitio.test.util;

import org.planit.input.InputBuilderListener;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.project.CustomPlanItProject;

public class TestOutputDto {
  
  private MemoryOutputFormatter memoryOutputFormatter;
  private CustomPlanItProject project;
  private InputBuilderListener inputBuilderListener;

  public TestOutputDto(final MemoryOutputFormatter memoryOutputFormatter, final CustomPlanItProject project,
      final InputBuilderListener inputBuilderListener) {
    this.memoryOutputFormatter = memoryOutputFormatter;
    this.project = project;
    this.inputBuilderListener = inputBuilderListener;
  }
  
  public MemoryOutputFormatter getMemoryOutputFormatter() {
    return memoryOutputFormatter;
  }

  public CustomPlanItProject getProject() {
    return project;
  }

  public InputBuilderListener getInputBuilderListener() {
    return inputBuilderListener;
  }

}
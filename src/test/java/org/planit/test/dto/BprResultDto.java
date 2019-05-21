package org.planit.test.dto;

import java.util.logging.Logger;

/**
 * DTO object containing extra fields specific to BPR function
 * 
 * @author gman6028
 *
 */
public class BprResultDto extends ResultDto {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(BprResultDto.class.getName());
        
 /**
  * Link capacity
  */
	private double capacity;
/**
 * Link length
 */
    private double length;
 /**
  * Link maximum speed
  */
    private double speed;
    
 /**
  * Constructor
  * 
 * @param startNodeId							id of start node 
 * @param endNodeId							id of end node
 * @param linkFlow								flow through link
 * @param linkCost								cost (travel time) of link
 * @param totalCostToEndNode			cumulative travel time from start of output path to the end of the current link
 * @param capacity								capacity of the link
 * @param length									length of the link
 * @param speed									travel speed of the link
 */
	public BprResultDto(long startNodeId, long endNodeId, double linkFlow, double linkCost, double totalCostToEndNode,
 			                             double capacity, double length, double speed) {
		super(startNodeId, endNodeId, linkFlow, linkCost, totalCostToEndNode);
		this.capacity = capacity;
		this.length = length;
		this.speed = speed;
	}

/**
 * Returns the capacity of this link
 * 
 * @return					capacity of this link
 */
	public double getCapacity() {
		return capacity;
	}

/**
 * Sets the capacity of this link
 * 
 * @param capacity				the capacity of this link
 */
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

/**
 * Returns the length of this link
 * 
 * @return			the length of this link
 */
	public double getLength() {
		return length;
	}

/**
 * Set the length of this link
 * 
 * @param length				the length of this link
 */
	public void setLength(double length) {
		this.length = length;
	}

/**
 * Return the travel speed for this link
 * 
 * @return			the travel speed for this link
 */
	public double getSpeed() {
		return speed;
	}

/**
 * Set the travel speed for this link
 * 
 * @param speed			the travel speed for this link
 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

}

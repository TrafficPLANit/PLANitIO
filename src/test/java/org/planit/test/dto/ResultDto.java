package org.planit.test.dto;

import java.util.logging.Logger;

/**
 * Data transfer object to store the results of runs
 * 
 * Each DTO stores details of one node in output path
 * 
 * @author gman6028
 *
 */
public class ResultDto implements Comparable<ResultDto> {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(ResultDto.class.getName());
        
 /**
  * Id of start node
  */
	private long startNodeId;
/**
 * Id of end node
 */
	private long endNodeId;
/**
 * Link flow
 */
	private double linkFlow;
/**
 * Calculated link cost
 */
	private double linkCost;
/**
 * Total travel costs from start of path to the end of this node
 */
	private Double totalCostToEndNode;  //use this to order results
/**
 * epsilon used for comparing doubles
 */
	private final static double epsilon = 0.0001; 
	
/**
 * Constructor to
 * 
 * @param startNodeId							id of start node 
 * @param endNodeId							id of end node
 * @param linkFlow								flow through link
 * @param linkCost								cost (travel time) of link
 * @param totalCostToEndNode			cumulative travel time from start of output path to the end of the current link
 */
	public ResultDto(long startNodeId, long endNodeId, double linkFlow, double linkCost, double  totalCostToEndNode) {
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.linkFlow = linkFlow;
		this.linkCost = linkCost;
		this.totalCostToEndNode = totalCostToEndNode;
	}
	
/**
 * Return the id of the start nonde
 * 
 * @return				id of start node
 */
	public long getStartNodeId() {
		return startNodeId;
	}
	
/**
 * Set the id of the end node
 * 
 * @param startNodeId		id of the start node
 */
	public void setStartNodeId(long startNodeId) {
		this.startNodeId = startNodeId;
	}
	
/**
 * Return the id of the end node
 * 
 * @return			id of the end node
 */
	public long getEndNodeId() {
		return endNodeId;
	}
	
/**
 * Set the id of the end node
 * 	
 * @param endNodeId			id of the end node
 */
	public void setEndNodeId(long endNodeId) {
		this.endNodeId = endNodeId;
	}
	
/**
 * Return the flow through this link
 * 
 * @return				the flow through this link
 */
	public double getLinkFlow() {
		return linkFlow;
	}
	
/**
 * Set the flow through this link
 * 
 * @param linkFlow		the flow through this link
 */
	public void setLinkFlow(double linkFlow) {
		this.linkFlow = linkFlow;
	}
	
/**
 * Get the cost for the current link
 * 
 * @return				the cost for the current link
 */
	public double getLinkCost() {
		return linkCost;
	}
	
/**
 * Set the cost for the current link
 * 
 * @param linkCost		the cost for the current link
 */
	public void setLinkCost(double linkCost) {
		this.linkCost = linkCost;
	}
	
/**
 * Get the cumulative cost of travel from the start of the output path to the end of the current link
 * 
 * @return				the cumulative cost of travel
 */
	public double getTotalCostToEndNode() {
		return  totalCostToEndNode;
	}
	
/**
 * Set the cumulative cost of travel from the start of the output path to the end of the current link
 * 
 * @param totalCostToEndNode			the cumulative cost of travel
 */
	public void setTotalCost(double totalCostToEndNode) {
		this.totalCostToEndNode =  totalCostToEndNode;
	}
	
/**
 * Compare this DTO with another using cumulative travel time
 * 
 * @param other				other ResultDto which is being compared to this one
 * @return						comparison of ResultDto objects based on cumulative travel time
 */
	@Override
	public int compareTo(ResultDto other) {
		return totalCostToEndNode.compareTo(other.getTotalCostToEndNode());
	}
	
/**
 * Tests whether this ResultDto object is equal to another one
 * 
 * @param other					ResultDto which is being compared to this one
 * @return							true if all fields in the DTO objects are equal, false otherwise
 */
	public boolean equals(ResultDto other) {
		if (startNodeId != other.getStartNodeId())
			return false;
		if (endNodeId != other.getEndNodeId())
			return false;
		if (Math.abs(linkCost - other.getLinkCost()) > epsilon)
			return false;
		if (Math.abs(linkFlow - other.getLinkFlow()) > epsilon)
			return false;
		if (Math.abs(totalCostToEndNode - other.getTotalCostToEndNode()) > epsilon)
			return false;
		return true;
	}
	
/**
 * Return the hashCode for this object
 * 
 * @return      hashCode for this object
 */
	public int hashCode() {
		double val = startNodeId + endNodeId * linkCost * linkFlow * totalCostToEndNode;
		return (int) Math.round(val);
	}
	
/**
 * Outputs the contents of this object as a String
 * 
 * @return				String showing the contents of this object
 */
	public String toString() {
		return "startNodeId = " + startNodeId + " endNodeId = " + endNodeId + " linkCost = " + linkCost + " linkFlow = " + linkFlow + " totalCostToEndNode = " + totalCostToEndNode;
	}

}

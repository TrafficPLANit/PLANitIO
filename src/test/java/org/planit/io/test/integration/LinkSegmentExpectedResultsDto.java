package org.planit.io.test.integration;

/**
 * DTO object containing extra fields specific to BPR function
 * 
 * February 2020:  At present only the speed, length, flow, cost and capacity properties are being used in testing.
 * 
 * @author gman6028
 *
 */
public class LinkSegmentExpectedResultsDto implements Comparable<LinkSegmentExpectedResultsDto> {

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
	 * External Id of start node
	 */
	private long startNodeId;
	/**
	 * External Id of end node
	 */
	private long endNodeId;
	/**
	 * Id of link segment
	 */
	private long linkSegmentId;
	
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
	private Double totalCostToEndNode; // use this to order results
	/**
	 * epsilon used for comparing doubles
	 */
	private final static double epsilon = 0.0001;

	/**
	 * Constructor
	 * 
	 * @param endNodeId       external id of end node
	 * @param startNodeId     external id of start node
	 * @param linkFlow           flow through link
	 * @param linkCost           cost (travel time) of link
	 * @param totalCostToEndNode cumulative travel time from start of output path to
	 *                           the end of the current link (optional)
	 * @param capacity           capacity of the link (no lanes x capacity per lane)
	 * @param length             length of the link
	 * @param speed              travel speed of the link
	 */
	public LinkSegmentExpectedResultsDto(long startNodeId, long endNodeId, double linkFlow, double linkCost, double totalCostToEndNode, double capacity, double length, double speed) {
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.linkSegmentId = -1;
		this.linkFlow = linkFlow;
		this.linkCost = linkCost;
		this.totalCostToEndNode = totalCostToEndNode;
		this.capacity = capacity;
		this.length = length;
		this.speed = speed;
	}

	/**
	 * Constructor
	 * 
	 * @param linkSegmentId    link segment id
	 * @param linkFlow           flow through link
	 * @param linkCost           cost (travel time) of link
	 * @param totalCostToEndNode cumulative travel time from start of output path to
	 *                           the end of the current link (optional)
	 * @param capacity           capacity of the link (no lanes x capacity per lane)
	 * @param length             length of the link
	 * @param speed              travel speed of the link
	 */
	public LinkSegmentExpectedResultsDto(long linkSegmentId, double linkFlow, double linkCost, double totalCostToEndNode, double capacity, double length, double speed) {
		this.startNodeId = -1;
		this.endNodeId = -1;
		this.linkSegmentId = linkSegmentId;
		this.linkFlow = linkFlow;
		this.linkCost = linkCost;
		this.totalCostToEndNode = totalCostToEndNode;
		this.capacity = capacity;
		this.length = length;
		this.speed = speed;
	}

	/**
	 * Returns the capacity of this link
	 * 
	 * @return capacity of this link
	 */
	public double getCapacity() {
		return capacity;
	}

	/**
	 * Sets the capacity of this link
	 * 
	 * @param capacity the capacity of this link
	 */
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	/**
	 * Returns the length of this link
	 * 
	 * @return the length of this link
	 */
	public double getLength() {
		return length;
	}

	/**
	 * Set the length of this link
	 * 
	 * @param length the length of this link
	 */
	public void setLength(double length) {
		this.length = length;
	}

	/**
	 * Return the travel speed for this link
	 * 
	 * @return the travel speed for this link
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Set the travel speed for this link
	 * 
	 * @param speed the travel speed for this link
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * Return the id of the start node
	 * 
	 * @return id of start node
	 */
	public long getStartNodeId() {
		return startNodeId;
	}

	/**
	 * Set the id of the end node
	 * 
	 * @param startNodeId id of the start node
	 */
	public void setStartNodeId(long startNodeId) {
		this.startNodeId = startNodeId;
	}

	/**
	 * Return the id of the end node
	 * 
	 * @return id of the end node
	 */
	public long getEndNodeId() {
		return endNodeId;
	}

	/**
	 * Set the id of the end node
	 * 
	 * @param endNodeId id of the end node
	 */
	public void setEndNodeId(long endNodeId) {
		this.endNodeId = endNodeId;
	}

	/**
	 * Return the flow through this link
	 * 
	 * @return the flow through this link
	 */
	public double getLinkFlow() {
		return linkFlow;
	}

	/**
	 * Set the flow through this link
	 * 
	 * @param linkFlow the flow through this link
	 */
	public void setLinkFlow(double linkFlow) {
		this.linkFlow = linkFlow;
	}

	/**
	 * Get the cost for the current link
	 * 
	 * @return the cost for the current link
	 */
	public double getLinkCost() {
		return linkCost;
	}

	/**
	 * Set the cost for the current link
	 * 
	 * @param linkCost the cost for the current link
	 */
	public void setLinkCost(double linkCost) {
		this.linkCost = linkCost;
	}

	/**
	 * Get the cumulative cost of travel from the start of the output path to the
	 * end of the current link
	 * 
	 * @return the cumulative cost of travel
	 */
	public double getTotalCostToEndNode() {
		return totalCostToEndNode;
	}

	/**
	 * Set the cumulative cost of travel from the start of the output path to the
	 * end of the current link
	 * 
	 * @param totalCostToEndNode the cumulative cost of travel
	 */
	public void setTotalCost(double totalCostToEndNode) {
		this.totalCostToEndNode = totalCostToEndNode;
	}
	
	/**
	 * Return the link segment Id
	 * 
	 * @return the link segment Id
	 */
  public long getLinkSegmentId() {
    return linkSegmentId;
  }

	/**
	 * Compare this DTO with another using cumulative travel time
	 * 
	 * @param other other ResultDto which is being compared to this one
	 * @return comparison of ResultDto objects based on cumulative travel time
	 */
	@Override
	public int compareTo(LinkSegmentExpectedResultsDto other) {
		return totalCostToEndNode.compareTo(other.getTotalCostToEndNode());
	}

	/**
	 * Tests whether this ResultDto object is equal to another one
	 * 
	 * @param other ResultDto which is being compared to this one
	 * @return true if all fields in the DTO objects are equal, false otherwise
	 */
	public boolean equals(LinkSegmentExpectedResultsDto other) {
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
	 * @return hashCode for this object
	 */
	public int hashCode() {
		double val = startNodeId + endNodeId * linkCost * linkFlow * totalCostToEndNode;
		return (int) Math.round(val);
	}

	/**
	 * Outputs the contents of this object as a String
	 * 
	 * @return String showing the contents of this object
	 */
	public String toString() {
		return "startNodeId = " + startNodeId + " endNodeId = " + endNodeId + " linkCost = " + linkCost + " linkFlow = "
				+ linkFlow + " totalCostToEndNode = " + totalCostToEndNode;
	}

}

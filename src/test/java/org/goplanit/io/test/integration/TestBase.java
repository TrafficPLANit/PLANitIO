package org.goplanit.io.test.integration;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.time.TimePeriod;

import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;

public abstract class TestBase {

  protected static final Path TEST_CASE_PATH = Path.of("src","test","resources","testcases");

  // numbered zones
  protected static final String zone1XmlId = "1";
  protected static final String zone2XmlId = "2";
  protected static final String zone3XmlId = "3";
  protected static final String zone4XmlId = "4";
  protected static final String zone5XmlId = "5";
  protected static final String zone6XmlId = "6";
  protected static final String zone27XmlId = "27";
  protected static final String zone31XmlId = "31";

  // named zones
  protected static final String zoneAXmlId = "A";
  protected static final String zoneBXmlId = "B";
  protected static final String zoneCXmlId = "C";
  protected static final String zoneDXmlId = "D";

  protected static final String node1XmlId = "1";
  protected static final String node2XmlId = "2";
  protected static final String node3XmlId = "3";
  protected static final String node4XmlId = "4";
  protected static final String node5XmlId = "5";
  protected static final String node6XmlId = "6";
  protected static final String node7XmlId = "7";
  protected static final String node8XmlId = "8";
  protected static final String node9XmlId = "9";
  protected static final String node10XmlId = "10";
  protected static final String node11XmlId = "11";
  protected static final String node12XmlId = "12";
  protected static final String node13XmlId = "13";
  protected static final String node14XmlId = "14";
  protected static final String node15XmlId = "15";
  protected static final String node16XmlId = "16";
  protected static final String node17XmlId = "17";
  protected static final String node18XmlId = "18";
  protected static final String node19XmlId = "19";
  protected static final String node20XmlId = "20";
  protected static final String node21XmlId = "21";
  protected static final String node22XmlId = "22";
  protected static final String node23XmlId = "23";
  protected static final String node24XmlId = "24";

  protected static final Long linkSegment0Id = 0L;
  protected static final Long linkSegment1Id = 1L;
  protected static final Long linkSegment2Id = 2L;
  protected static final Long linkSegment3Id = 3L;
  protected static final Long linkSegment4Id = 4L;
  protected static final Long linkSegment5Id = 5L;
  protected static final Long linkSegment6Id = 6L;

  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */
  protected Map<TimePeriod, Map<Mode, Map<String, Map<String, String>>>> pathMap;
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, od value */
  protected Map<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>> odMap;
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, result DTO */
  protected SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> linkResults;
  /* TODO: refactor UGLY: timeperiod, mode origin link segment id, result DTO */
  protected SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> linkIdResultsMap;

}

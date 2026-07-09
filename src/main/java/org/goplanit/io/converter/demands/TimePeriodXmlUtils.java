package org.goplanit.io.converter.demands;

import org.goplanit.demands.TimePeriods;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.xml.generated.v2.Durationunit;
import org.goplanit.xml.generated.v2.TimePeriodType;
import org.goplanit.xml.generated.v2.TimePeriodsContainerType;
import org.goplanit.xml.generated.v2.XMLElementDuration;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.function.Function;
import java.util.logging.Logger;

public class TimePeriodXmlUtils {

  private static final Logger LOGGER = Logger.getLogger(TimePeriodXmlUtils.class.getCanonicalName());

  /**
   * Add a time period to the XML container element
   *
   * @param xmlTimePeriods     to add to
   * @param timePeriod         to add
   * @param timePeriodIdMapper to use
   */
  public static void addTimePeriod(
      TimePeriodsContainerType xmlTimePeriods,
      TimePeriod timePeriod,
      Function<TimePeriod, String> timePeriodIdMapper) {
    var xmlTimePeriod = new TimePeriodType();
    xmlTimePeriods.getTimeperiods().add(xmlTimePeriod);

    /* XML id */
    xmlTimePeriod.setId(timePeriodIdMapper.apply(timePeriod));

    /* external id */
    if(timePeriod.hasExternalId()) {
      xmlTimePeriod.setExternalid(timePeriod.getExternalId());
    }

    /* name/description */
    if(timePeriod.hasDescription()) {
      xmlTimePeriod.setName(timePeriod.getDescription());
    }

    /* start time */
    if(timePeriod.getStartTimeSeconds() > 0) {
      try {
        xmlTimePeriod.setStarttime(LocalTime.ofSecondOfDay(timePeriod.getStartTimeSeconds()));
      } catch (Exception e) {
        LOGGER.severe(e.getMessage());
        throw new PlanItRunTimeException("Error when generating start time of time period "+ timePeriod.getXmlId()+
            " when persisting demand configuration",e);
      }
    }

    /* duration */
    if(timePeriod.getDurationSeconds()<=0) {
      throw new PlanItRunTimeException("Error duration of time period %s  is not positive, this is not allowed",
          timePeriod.getXmlId());
    }
    var xmlDuration = new XMLElementDuration();
    xmlDuration.setUnit(Durationunit.S); // TODO: ideally we keep the original unit so input and output files are consistent
    xmlDuration.setValue(BigInteger.valueOf(timePeriod.getDurationSeconds()));
    xmlTimePeriod.setDuration(xmlDuration);
  }

  /**
   * Parse time period and register on the time periods container.
   *
   * @param xmlTimePeriod    to extract from
   * @param defaultStartTime to use
   * @param timePeriods      to register it on
   * @return created time period
   */
  public static TimePeriod parseTimePeriod(
      TimePeriodType xmlTimePeriod, LocalTime defaultStartTime, TimePeriods timePeriods) {
    /* starttime, duration */
    int startTimeSeconds = (xmlTimePeriod.getStarttime() == null) ?
        defaultStartTime.toSecondOfDay() : xmlTimePeriod.getStarttime().toSecondOfDay();
    int duration = xmlTimePeriod.getDuration().getValue().intValue();
    Durationunit durationUnit = xmlTimePeriod.getDuration().getUnit();
    if (xmlTimePeriod.getName() == null) {
      xmlTimePeriod.setName("");
    }
    switch (durationUnit) {
      case H:
        duration *= 3600;
        break;
      case M:
        duration *= 60;
        break;
      case S:
        break;
    }

    /* PLANit time period */
    TimePeriod timePeriod = timePeriods.getFactory().registerNew(
        xmlTimePeriod.getName(), startTimeSeconds, duration /*converted to seconds*/);

    /* xml id */
    if(xmlTimePeriod.getId() != null && !xmlTimePeriod.getId().isBlank()) {
      timePeriod.setXmlId(xmlTimePeriod.getId());
    }

    /* external id */
    if(xmlTimePeriod.getExternalid() != null && !xmlTimePeriod.getExternalid().isBlank()) {
      timePeriod.setExternalId(xmlTimePeriod.getExternalid());
    }

    return timePeriod;
  }
}

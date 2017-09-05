/*
  Copyright 2017 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.​

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
*/

package com.esri.geoevent.processor.mostrecentcacheiterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.esri.ges.core.Uri;
import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.geoevent.GeoEventPropertyName;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.messaging.EventDestination;
import com.esri.ges.messaging.EventUpdatable;
import com.esri.ges.messaging.GeoEventCreator;
import com.esri.ges.messaging.GeoEventProducer;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;
import com.esri.ges.util.Converter;

public class MostRecentCacheIterator extends GeoEventProcessorBase implements GeoEventProducer, EventUpdatable
{
  private static final BundleLogger   LOGGER      = BundleLoggerFactory.getLogger(MostRecentCacheIterator.class);

  private long                        cycleInterval;
  private long                        messageInterval;

  private final Map<String, GeoEvent> trackCache  = new ConcurrentHashMap<String, GeoEvent>();

  private Messaging                   messaging;
  private GeoEventCreator             geoEventCreator;
  private GeoEventProducer            geoEventProducer;
  private Date                        clearCacheTime;
  private boolean                     autoClearCache;
  private boolean                     clearCacheOnStart;
  private Timer                       clearCacheTimer;
  private String                      categoryField;
  private Uri                         definitionUri;
  private String                      definitionUriString;
  private boolean                     isIterating = false;
  final Object                        lock1       = new Object();

  class ClearCacheTask extends TimerTask
  {
    public void run()
    {
      // clear the cache
      if (autoClearCache == true)
      {
        trackCache.clear();
      }
    }
  }

  class CacheIterator implements Runnable
  {
    private Long cycleInterval   = 5000L;
    private Long messageInterval = 1L;

    public CacheIterator(String category, Long cycleInterval, Long messageInterval)
    {
      this.cycleInterval = cycleInterval;
      this.messageInterval = messageInterval;
    }

    @Override
    public void run()
    {
      while (isIterating)
      {
        try
        {
          Thread.sleep(cycleInterval);

          for (String catId : trackCache.keySet())
          {
            GeoEvent geoEvent = trackCache.get(catId);
            try
            {
              createGeoEventAndSend(geoEvent);
              Thread.sleep(messageInterval);
            }
            catch (MessagingException error)
            {
              LOGGER.error("SEND_ERROR", catId, error.getMessage());
              LOGGER.info(error.getMessage(), error);
            }
          }
        }
        catch (InterruptedException error)
        {
          LOGGER.error(error.getMessage(), error);
        }
      }
    }
  }

  protected MostRecentCacheIterator(GeoEventProcessorDefinition definition) throws ComponentException
  {
    super(definition);
  }

  public void afterPropertiesSet()
  {
    messageInterval = Converter.convertToInteger(getProperty("messageInterval").getValueAsString(), 10);
    cycleInterval = Converter.convertToInteger(getProperty("cycleInterval").getValueAsString(), 10);
    String[] resetTimeStr = getProperty("clearCacheTime").getValueAsString().split(":");
    // Get the Date corresponding to 11:01:00 pm today.
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(resetTimeStr[0]));
    calendar.set(Calendar.MINUTE, Integer.parseInt(resetTimeStr[1]));
    calendar.set(Calendar.SECOND, Integer.parseInt(resetTimeStr[2]));
    clearCacheTime = calendar.getTime();
    autoClearCache = Converter.convertToBoolean(getProperty("autoClearCache").getValueAsString());
    clearCacheOnStart = Converter.convertToBoolean(getProperty("clearCacheOnStart").getValueAsString());
  }

  @Override
  public void setId(String id)
  {
    super.setId(id);
    geoEventProducer = messaging.createGeoEventProducer(new EventDestination(id + ":event"));
  }

  @Override
  public GeoEvent process(GeoEvent geoEvent) throws Exception
  {
    String trackId = geoEvent.getTrackId();

    // Need to synchronize the Concurrent Map on write to avoid wrong counting
    synchronized (lock1)
    {
      // Add or update the status cache
      trackCache.put(trackId, geoEvent);
    }

    return null;
  }

  private void createGeoEventAndSend(GeoEvent sourceGeoEvent) throws MessagingException
  {
    GeoEvent geoEventOut = (GeoEvent) sourceGeoEvent.clone();
    geoEventOut.setProperty(GeoEventPropertyName.TYPE, "event");
    geoEventOut.setProperty(GeoEventPropertyName.OWNER_ID, getId());
    geoEventOut.setProperty(GeoEventPropertyName.OWNER_URI, definition.getUri());
    for (Map.Entry<GeoEventPropertyName, Object> property : sourceGeoEvent.getProperties())
    {
      if (!geoEventOut.hasProperty(property.getKey()))
      {
        geoEventOut.setProperty(property.getKey(), property.getValue());
      }
    }
    send(geoEventOut);
  }

  @Override
  public List<EventDestination> getEventDestinations()
  {
    return (geoEventProducer != null) ? Arrays.asList(geoEventProducer.getEventDestination()) : new ArrayList<EventDestination>();
  }

  @Override
  public void validate() throws ValidationException
  {
    super.validate();
    List<String> errors = new ArrayList<String>();
    if (cycleInterval <= 0)
      errors.add(LOGGER.translate("VALIDATION_INVALID_REPORT_INTERVAL", definition.getName()));
    if (messageInterval <= 0)
      errors.add(LOGGER.translate("VALIDATION_INVALID_REPORT_INTERVAL", definition.getName()));
    if (errors.size() > 0)
    {
      StringBuffer sb = new StringBuffer();
      for (String message : errors)
        sb.append(message).append("\n");
      throw new ValidationException(LOGGER.translate("VALIDATION_ERROR", this.getClass().getName(), sb.toString()));
    }
  }

  @Override
  public void onServiceStart()
  {
    if (this.clearCacheOnStart == true)
    {
      if (clearCacheTimer == null)
      {
        // Get the Date corresponding to 11:01:00 pm today.
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(clearCacheTime);
        Date time1 = calendar1.getTime();

        clearCacheTimer = new Timer();
        Long dayInMilliSeconds = 60 * 60 * 24 * 1000L;
        clearCacheTimer.scheduleAtFixedRate(new ClearCacheTask(), time1, dayInMilliSeconds);
      }
      trackCache.clear();
    }

    isIterating = true;
    if (definition != null)
    {
      definitionUri = definition.getUri();
      definitionUriString = definitionUri.toString();
    }

    CacheIterator reportGen = new CacheIterator(categoryField, cycleInterval, messageInterval);
    Thread thread = new Thread(reportGen);
    thread.setName("MostRecentCacheIterator Report Generator");
    thread.start();
  }

  @Override
  public void onServiceStop()
  {
    if (clearCacheTimer != null)
    {
      clearCacheTimer.cancel();
    }
    isIterating = false;
  }

  @Override
  public void shutdown()
  {
    super.shutdown();

    if (clearCacheTimer != null)
    {
      clearCacheTimer.cancel();
    }
  }

  @Override
  public EventDestination getEventDestination()
  {
    return (geoEventProducer != null) ? geoEventProducer.getEventDestination() : null;
  }

  @Override
  public void send(GeoEvent geoEvent) throws MessagingException
  {
    if (geoEventProducer != null && geoEvent != null)
    {
      geoEventProducer.send(geoEvent);
    }
  }

  public void setMessaging(Messaging messaging)
  {
    this.messaging = messaging;
    geoEventCreator = messaging.createGeoEventCreator();
  }

  @Override
  public void disconnect()
  {
    if (geoEventProducer != null)
      geoEventProducer.disconnect();
  }

  @Override
  public String getStatusDetails()
  {
    return (geoEventProducer != null) ? geoEventProducer.getStatusDetails() : "";
  }

  @Override
  public void init() throws MessagingException
  {
    afterPropertiesSet();
  }

  @Override
  public boolean isConnected()
  {
    return (geoEventProducer != null) ? geoEventProducer.isConnected() : false;
  }

  @Override
  public void setup() throws MessagingException
  {
    ;
  }

  @Override
  public void update(Observable o, Object arg)
  {
    ;
  }
}

package com.esri.geoevent.processor.mostrecentcacheiterator;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.processor.GeoEventProcessor;
import com.esri.ges.processor.GeoEventProcessorServiceBase;

public class MostRecentCacheIteratorService extends GeoEventProcessorServiceBase
{
  private Messaging messaging;

  public MostRecentCacheIteratorService()
  {
    definition = new MostRecentCacheIteratorDefinition();
  }

  @Override
  public GeoEventProcessor create() throws ComponentException
  {
    MostRecentCacheIterator detector = new MostRecentCacheIterator(definition);
    detector.setMessaging(messaging);
    return detector;
  }

  public void setMessaging(Messaging messaging)
  {
    this.messaging = messaging;
  }
}
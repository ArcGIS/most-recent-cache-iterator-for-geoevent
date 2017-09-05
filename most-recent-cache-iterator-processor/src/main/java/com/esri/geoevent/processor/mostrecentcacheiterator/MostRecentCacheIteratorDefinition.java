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
import java.util.List;

import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.DefaultGeoEventDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.property.LabeledValue;
import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.processor.GeoEventProcessorDefinitionBase;

public class MostRecentCacheIteratorDefinition extends GeoEventProcessorDefinitionBase
{
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(MostRecentCacheIteratorDefinition.class);

  public MostRecentCacheIteratorDefinition()
  {
    try
    {
      propertyDefinitions.put("cycleInterval", new PropertyDefinition("cycleInterval", PropertyType.Long, 1000, "Cycle Interval (milliseconds)", "Cycle Interval (milliseconds)", false, false));
      propertyDefinitions.put("messageInterval", new PropertyDefinition("messageInterval", PropertyType.Long, 10, "Message Interval (milliseconds)", "Message Interval (milliseconds)", false, false));
      propertyDefinitions.put("clearCacheOnStart", new PropertyDefinition("clearCacheOnStart", PropertyType.Boolean, false, "Clear Cache on Start", "Clear Cache on Start", true, false));
      propertyDefinitions.put("autoClearCache", new PropertyDefinition("autoClearCache", PropertyType.Boolean, false, "Automatic Clear Cache", "Auto Clear Cache", true, false));
      propertyDefinitions.put("clearCacheTime", new PropertyDefinition("clearCacheTime", PropertyType.String, "00:00:00", "Clear cache time", "Clear cache time", "autoClearCache=true", false, false));
    }
    catch (Exception error)
    {
      LOGGER.error("INIT_ERROR", error.getMessage());
      LOGGER.info(error.getMessage(), error);
    }
  }

  @Override
  public String getName()
  {
    return "MostRecentCacheIterator";
  }

  @Override
  public String getDomain()
  {
    return "com.esri.geoevent.processor";
  }

  @Override
  public String getVersion()
  {
    return "10.5.0";
  }

  @Override
  public String getLabel()
  {
    return "${com.esri.geoevent.processor.most-recent-cache-iterator-processor.PROCESSOR_LABEL}";
  }

  @Override
  public String getDescription()
  {
    return "${com.esri.geoevent.processor.most-recent-cache-iterator-processor.PROCESSOR_DESC}";
  }
}

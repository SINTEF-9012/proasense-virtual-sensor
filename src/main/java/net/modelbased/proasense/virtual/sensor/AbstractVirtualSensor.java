/**
 * Copyright (C) 2014-2015 SINTEF
 *
 *     Brian Elvesæter <brian.elvesater@sintef.no>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.modelbased.proasense.virtual.sensor;

import eu.proasense.internal.SimpleEvent;
import net.modelbased.proasense.adapter.base.AbstractBaseAdapter;

import java.util.concurrent.BlockingQueue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public abstract class AbstractVirtualSensor extends AbstractBaseAdapter {
    protected BlockingQueue<SimpleEvent> queue;
    protected KafkaConsumerInput inputPort;
    protected String propertyName;
    protected String propertyType;
    protected int samplingRate;
    protected int rateMin;
    protected int rateMax;


    public AbstractVirtualSensor() {
        // Kafka input port properties
        String zooKeeper = adapterProperties.getProperty("zookeeper.connect");
        String groupId = adapterProperties.getProperty("kafka.groupid");
        String topic = adapterProperties.getProperty("proasense.virtual.sensor.incoming.topic");

        inputPort = new KafkaConsumerInput(queue, zooKeeper, groupId, topic, adapterProperties);

        // Virtual sensor properties
        this.propertyName = adapterProperties.getProperty("proasense.virtual.sensor.property.name");
        this.propertyType = adapterProperties.getProperty("proasense.virtual.sensor.property.type");
        this.samplingRate = new Integer(adapterProperties.getProperty("proasense.virtual.sensor.rate.default")).intValue();
        this.rateMin = new Integer(adapterProperties.getProperty("proasense.virtual.sensor.rate.min")).intValue();
        this.rateMax = new Integer(adapterProperties.getProperty("proasense.virtual.sensor.rate.max")).intValue();
    }

    // RESTful interface
    public void changeSamplingRate(int newSamplingRate) {
        if (newSamplingRate < this.rateMin) {
            this.samplingRate = this.rateMin;
        }
        else if (newSamplingRate > this.rateMax) {
            this.samplingRate = this.rateMax;
        }
        else
            this.samplingRate = newSamplingRate;
    }

    @GET
    @Path("/adapter/status")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getServerStatus() {
        String result = "Virtual Sensor running...";

        // Return HTTP response 200 in case of success
        return Response.status(200).entity(result).build();
    }


    @GET
    @Path("/adapter/properties")
    @Produces(MediaType.TEXT_PLAIN)
    public Response printServerProperties() {
        String result = this.adapterProperties.toString();

        // Return HTTP response 200 in case of success
        return Response.status(200).entity(result).build();
    }

}

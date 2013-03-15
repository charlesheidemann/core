package org.jboss.forge.container.impl;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;

import org.jboss.forge.container.addons.Addon;

@Singleton
public class AddonProducer
{
   private Addon addon;

   @Produces
   @Singleton
   @Typed(Addon.class)
   public Addon produceCurrentAddon()
   {
      return addon;
   }

   public void setAddon(Addon addon)
   {
      this.addon = addon;
   }
}

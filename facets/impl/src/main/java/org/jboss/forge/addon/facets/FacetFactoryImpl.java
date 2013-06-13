/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.facets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.forge.addon.facets.constraints.FacetInspector;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FacetFactoryImpl implements FacetFactory
{
   @Inject
   private AddonRegistry registry;

   @Override
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> FACETTYPE create(
            Class<FACETTYPE> type)
   {
      Assert.notNull(type, "Facet type must not be null.");
      ExportedInstance<FACETTYPE> instance = registry.getExportedInstance(type);
      if (instance == null)
         throw new FacetNotFoundException("Could not find Facet of type [" + type.getName() + "]");
      return instance.get();
   }

   @Override
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> FACETTYPE create(
            Class<FACETTYPE> type, FACETEDTYPE origin)
   {
      FACETTYPE instance = create(type);
      if (instance instanceof MutableOrigin)
         ((MutableOrigin<FACETEDTYPE, FACETTYPE>) instance).setOrigin(origin);
      else
         throw new IllegalArgumentException("Facet type [" + type.getName() + "] does not support setting an origin.");
      return instance;
   }

   @Override
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> Iterable<FACETTYPE> createFacets(
            Class<FACETTYPE> type)
   {
      Assert.notNull(type, "Facet type must not be null.");
      Set<ExportedInstance<FACETTYPE>> instances = registry.getExportedInstances(type);
      Set<FACETTYPE> facets = new HashSet<FACETTYPE>(instances.size());
      for (ExportedInstance<FACETTYPE> instance : instances)
      {
         facets.add(instance.get());
      }
      return facets;
   }

   @Override
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> Iterable<FACETTYPE> createFacets(
            Class<FACETTYPE> type, FACETEDTYPE origin)
   {
      Iterable<FACETTYPE> facets = createFacets(type);
      for (FACETTYPE facet : facets)
      {
         if (facet instanceof MutableOrigin)
            ((MutableOrigin<FACETEDTYPE, FACETTYPE>) facet).setOrigin(origin);
         else
            throw new IllegalArgumentException("Facet type [" + type.getName()
                     + "] does not support setting an origin.");
      }
      return facets;
   }

   @Override
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> FACETTYPE install(
            Class<FACETTYPE> type, FACETEDTYPE origin)
            throws FacetNotFoundException
   {
      FACETTYPE facet = create(type, origin);
      if (!install(facet, origin))
      {
         throw new IllegalStateException("Facet type [" + type.getName()
                  + "] could not be installed completely into [" + origin
                  + "] of type [" + origin.getClass().getName()
                  + "]. You may wish to check for inconsistent origin state.");
      }
      return facet;
   }

   @Override
   @SuppressWarnings("unchecked")
   public <FACETEDTYPE extends Faceted<FACETTYPE, FACETEDTYPE>, FACETTYPE extends Facet<FACETEDTYPE, FACETTYPE>> boolean install(
            FACETTYPE facet, FACETEDTYPE origin)
   {
      Assert.notNull(origin, "Facet instance must not be null.");
      Assert.notNull(origin, "Origin instance must not be null.");

      Faceted<FACETTYPE, FACETEDTYPE> faceted = (Faceted<FACETTYPE, FACETEDTYPE>) origin;
      Assert.isTrue(faceted instanceof MutableFaceted, "The given origin [" + origin + "] is not an instance of ["
               + MutableFaceted.class.getName() + "], and does not support " + Facet.class.getSimpleName()
               + " installation.");

      if (facet.getOrigin() == null && facet instanceof MutableOrigin)
      {
         ((MutableOrigin<FACETEDTYPE, FACETTYPE>) facet).setOrigin(origin);
      }

      Assert.isTrue(origin.equals(facet.getOrigin()), "The given origin [" + origin + "] is not an instance of ["
               + MutableFaceted.class.getName() + "], and does not support " + Facet.class.getSimpleName()
               + " installation.");

      List<Class<FACETTYPE>> requiredFacets = FacetInspector.getRequiredFacets(facet.getClass());
      List<Class<FACETTYPE>> facetsToInstall = new ArrayList<Class<FACETTYPE>>();
      for (Class<FACETTYPE> requirement : requiredFacets)
      {
         if (!origin.hasFacet(requirement))
         {
            facetsToInstall.add(requirement);
         }
      }

      for (Class<FACETTYPE> requirement : facetsToInstall)
      {
         install(requirement, origin);
      }

      boolean result = false;
      if (faceted.hasFacet((Class<? extends FACETTYPE>) facet.getClass()))
         result = true;
      else
         result = ((MutableFaceted<FACETEDTYPE, FACETTYPE>) faceted).install(facet);
      return result;
   }
}

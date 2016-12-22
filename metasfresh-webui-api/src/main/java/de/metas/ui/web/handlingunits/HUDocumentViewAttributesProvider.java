package de.metas.ui.web.handlingunits;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.mm.attributes.spi.IAttributeValueContext;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.adempiere.util.lang.ExtendedMemorizingSupplier;
import org.compiere.util.Env;

import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.attribute.IAttributeValue;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactoryService;
import de.metas.handlingunits.attribute.storage.IAttributeStorageListener;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.storage.IHUStorageFactory;
import de.metas.ui.web.window.controller.Execution;
import de.metas.ui.web.window.controller.IDocumentViewAttributes;
import de.metas.ui.web.window.controller.IDocumentViewAttributesProvider;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.model.IDocumentChangesCollector;
import de.metas.ui.web.window.model.MutableDocumentFieldChangedEvent;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class HUDocumentViewAttributesProvider implements IDocumentViewAttributesProvider
{
	private final Supplier<IAttributeStorageFactory> _attributeStorageFactory = ExtendedMemorizingSupplier.of(() -> createAttributeStorageFactory());
	private final ConcurrentHashMap<Integer, HUDocumentViewAttributes> documentId2attributes = new ConcurrentHashMap<>();

	public HUDocumentViewAttributesProvider()
	{
		super();
	}

	@Override
	public IDocumentViewAttributes getAttributes(final int documentId)
	{
		return documentId2attributes.computeIfAbsent(documentId, key -> createDocumentViewAttributes(documentId));
	}

	private HUDocumentViewAttributes createDocumentViewAttributes(final int documentId)
	{
		final int huId = documentId;
		final I_M_HU hu = InterfaceWrapperHelper.create(Env.getCtx(), huId, I_M_HU.class, ITrx.TRXNAME_None);
		if (hu == null)
		{
			throw new IllegalArgumentException("No HU found for M_HU_ID=" + huId);
		}

		final IAttributeStorage attributesStorage = getAttributeStorageFactory().getAttributeStorage(hu);
		return new HUDocumentViewAttributes(attributesStorage);
	}

	public IAttributeStorageFactory getAttributeStorageFactory()
	{
		return _attributeStorageFactory.get();
	}

	private final IAttributeStorageFactory createAttributeStorageFactory()
	{
		final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
		final IAttributeStorageFactoryService attributeStorageFactoryService = Services.get(IAttributeStorageFactoryService.class);

		final IHUStorageFactory storageFactory = handlingUnitsBL.getStorageFactory();
		final IAttributeStorageFactory huAttributeStorageFactory = attributeStorageFactoryService.createHUAttributeStorageFactory();
		huAttributeStorageFactory.setHUStorageFactory(storageFactory);

		huAttributeStorageFactory.addAttributeStorageListener(AttributeStorage2ExecutionEventsForwarder.instance);

		return huAttributeStorageFactory;
	}

	private static final class AttributeStorage2ExecutionEventsForwarder implements IAttributeStorageListener
	{
		public static final transient HUDocumentViewAttributesProvider.AttributeStorage2ExecutionEventsForwarder instance = new HUDocumentViewAttributesProvider.AttributeStorage2ExecutionEventsForwarder();

		private AttributeStorage2ExecutionEventsForwarder()
		{
			super();
		}

		private final void forwardEvent(final IAttributeStorage storage, final IAttributeValue attributeValue)
		{
			final IDocumentChangesCollector documentChangesCollector = Execution.getCurrentDocumentChangesCollector();

			final DocumentPath documentPath = HUDocumentViewAttributesHelper.extractDocumentPath(storage);
			final String attributeName = HUDocumentViewAttributesHelper.extractAttributeName(attributeValue);
			final Object jsonValue = HUDocumentViewAttributesHelper.extractJSONValue(attributeValue);

			documentChangesCollector.collectEvent(MutableDocumentFieldChangedEvent.of(documentPath, attributeName)
					.setValue(jsonValue));
		}

		@Override
		public void onAttributeValueCreated(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue)
		{
			forwardEvent(storage, attributeValue);
		}

		@Override
		public void onAttributeValueChanged(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue, final Object valueOld)
		{
			forwardEvent(storage, attributeValue);
		}

		@Override
		public void onAttributeValueDeleted(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void onAttributeStorageDisposed(final IAttributeStorage storage)
		{
			// nothing
		}

	}
}
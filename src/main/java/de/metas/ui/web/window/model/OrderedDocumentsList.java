package de.metas.ui.web.window.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Mutable ordered documents list.
 * 
 * It also contains {@link #getOrderBys()}.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@ToString
public final class OrderedDocumentsList
{
	public static final OrderedDocumentsList of(final Collection<Document> documents, final List<DocumentQueryOrderBy> orderBys)
	{
		return new OrderedDocumentsList(documents, orderBys);
	}

	public static final OrderedDocumentsList newEmpty(final List<DocumentQueryOrderBy> orderBys)
	{
		return new OrderedDocumentsList(ImmutableList.of(), orderBys);
	}

	private final List<Document> documents;
	private final List<DocumentQueryOrderBy> orderBys;

	private OrderedDocumentsList(final Collection<Document> documents, final List<DocumentQueryOrderBy> orderBys)
	{
		this.documents = documents == null ? new ArrayList<>() : new ArrayList<>(documents);
		this.orderBys = ImmutableList.copyOf(orderBys);
	}

	public List<Document> toList()
	{
		return documents;
	}

	public void addDocument(@NonNull final Document document)
	{
		documents.add(document);
	}

	public void addDocuments(@NonNull final Collection<Document> documents)
	{
		if (documents.isEmpty())
		{
			return;
		}

		documents.forEach(this::addDocument);
	}

	public int size()
	{
		return documents.size();
	}

	public boolean isEmpty()
	{
		return documents.isEmpty();
	}

	public Document get(final int index)
	{
		return documents.get(index);
	}

	public List<DocumentQueryOrderBy> getOrderBys()
	{
		return orderBys;
	}
}

package org.openmrs.module.sync2.client.reader.atomfeed;

import org.ict4h.atomfeed.client.domain.Event;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.api.filter.FeedFilter;
import org.openmrs.module.atomfeed.api.filter.GenericFeedFilterStrategy;
import org.openmrs.module.atomfeed.api.service.TagService;
import org.openmrs.module.atomfeed.client.FeedEventWorker;
import org.openmrs.module.sync2.SyncConstants;
import org.openmrs.module.sync2.api.helper.CategoryHelper;
import org.openmrs.module.sync2.api.model.enums.AtomfeedTagContent;
import org.openmrs.module.sync2.api.service.SyncPullService;
import org.openmrs.module.sync2.api.utils.ContextUtils;
import org.openmrs.module.sync2.api.utils.SyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParentAtomfeedFeedWorker implements FeedEventWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParentAtomfeedFeedWorker.class);

	@Override
	public void process(Event event) {
		LOGGER.info("Started feed event processing (id: {})", event.getId());
		SyncPullService pullService = ContextUtils.getSyncPullService();
		CategoryHelper categoryHelper = ContextUtils.getCategoryHelper();
		List tags = event.getCategories();

		TagService tagService = Context.getRegisteredComponent(SyncConstants.TAG_SERVICE_BEAN, TagService.class);
		List<FeedFilter> feedFilters = tagService.getFeedFiltersFromTags(tags);

		boolean shouldBeSynced = true;
		for (FeedFilter feedFilter : feedFilters) {
			GenericFeedFilterStrategy bean;
			try {
				bean = Context.getRegisteredComponent(feedFilter.getBeanName(), GenericFeedFilterStrategy.class);
			} catch (APIException e) {
				LOGGER.warn("Bean not found: {}", feedFilter.getBeanName());
				shouldBeSynced = true;
				break;
			}
			if (!bean.isFilterTagValid(feedFilter.getFilter())) {
				shouldBeSynced = false;
				break;
			}
		}

		if (shouldBeSynced) {
			pullService.pullAndSaveObjectFromParent(
					categoryHelper.getByCategory(SyncUtils.getValueOfAtomfeedEventTag(tags, AtomfeedTagContent.CATEGORY)),
					SyncUtils.getLinks(event.getContent()),
					SyncUtils.getValueOfAtomfeedEventTag(tags, AtomfeedTagContent.EVENT_ACTION)
			);
		}
	}

	@Override
	public void cleanUp(Event event) {
		LOGGER.info("Started feed cleanUp processing (id: {})", event.getId());
	}
}

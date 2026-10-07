package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedService {

    private final PostRepository posts;
    private final Map<FeedSort, FeedRankingStrategy> strategies;

    public FeedService(PostRepository posts, List<FeedRankingStrategy> strategies) {
        this.posts = posts;
        this.strategies = new EnumMap<>(FeedSort.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.sort(), strategy));
    }

    public List<Post> listFeed(FeedSort sort, int page, int size) {
        FeedSort requestedSort = sort == null ? FeedSort.NEW : sort;
        FeedRankingStrategy strategy = strategies.get(requestedSort);
        if (strategy == null) {
            strategy = strategies.getOrDefault(FeedSort.NEW, new FeedRankingStrategy() {
                @Override
                public FeedSort sort() {
                    return FeedSort.NEW;
                }

                @Override
                public List<Post> fetch(PostRepository repository, int page, int size) {
                    return repository.findLatest(page, size);
                }
            });
        }
        return strategy.fetch(posts, page, size);
    }
}

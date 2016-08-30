
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * Created by izhaowo on 2016/6/2.
 */
public class CardViewPager extends RecyclerView {
    WrapperAdapter wrapperAdapter;
    /**
     * item间距的像素
     */
    int itemPadding = 0;
    private int scrolled = 0;
    int mMinFlingVelocity;

    public CardViewPager(Context context) {
        super(context);
        init(context);
    }

    public CardViewPager(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CardViewPager(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    boolean innerScroll = false;
    private void init(Context context) {
        if(isInEditMode()){return;}
        ViewConfiguration vc = ViewConfiguration.get(context);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        setOverScrollMode(OVER_SCROLL_NEVER);
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setWrapperAdapter(new WrapperAdapter(this, defaultCardAdapter));
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                scrolled += dx;
            }
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState){
                    case RecyclerView.SCROLL_STATE_DRAGGING:{
                        innerScroll = false;
                        break;
                    }
                    case RecyclerView.SCROLL_STATE_IDLE:{
                        if (innerScroll) {
                            View currentCenterView = getCenterChild();
                            if (currentCenterView == null){return;}
                            final LayoutManager layoutManager = getLayoutManager();
                            final int currentPosition = layoutManager.getPosition(currentCenterView);
                            wrapperAdapter.onPageChanged(currentPosition);
                        }
                        break;
                    }
                    case RecyclerView.SCROLL_STATE_SETTLING:{
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void stopNestedScroll() {
        super.stopNestedScroll();

        if(!innerScroll){
            scrollToCard(0);
        }
    }

    private void scrollToCard(int pageOffset){
        View currentCenterView = getCenterChild();
        if (currentCenterView == null){return;}

        final LayoutManager layoutManager = getLayoutManager();
        final int currentPosition = layoutManager.getPosition(currentCenterView);
        if(currentPosition == 0 && pageOffset < 0){
            scrollToCard(0);
            return;
        }
        final int itemCount = wrapperAdapter.getItemCount();
        if(currentPosition == itemCount - 1 && pageOffset > 0){
            scrollToCard(itemCount - 1);
            return;
        }

        final int targetPosition = currentPosition + pageOffset;
        float left = 0;
        for(int i= 0; i < targetPosition; i++){
            left += wrapperAdapter.getItemWidth(i);
        }
        final int toLeft = (int) (left * getWidth() + targetPosition * getItemPadding());
        final int toCenter = (int) (toLeft + (getWidth() * wrapperAdapter.getItemWidth(targetPosition)) * .5f);// + getItemPadding()

        innerScroll = true;
        smoothScrollBy((int) (toCenter - scrolled - getWidth() * .5f), 0);
    }

    @Nullable
    private View getCenterChild() {
        final int halfPadding = (int) (getItemPadding() * .5f);
        final float center = getWidth() * .5f;
        final int childCount = getChildCount();
        for(int i = childCount-1; i >= 0; i--){
            final View childAt = getChildAt(i);
            if(childAt == null){continue;}
            final int left = childAt.getLeft() - halfPadding;
            final int right = childAt.getRight() + halfPadding;
            if(left <= center && right >= center){
                return childAt;
            }
        }
        return null;
    }

    public void scrollTo(View view) {
        float centerView = (view.getLeft() + view.getRight()) * 0.5f;
        final int width = getWidth();

        innerScroll = true;
        smoothScrollBy(-(int) (width*.5f - centerView), 0);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if(isLayoutFrozen()){return false;}
        LayoutManager layoutManager = getLayoutManager();
        if(layoutManager == null){return false;}
        final boolean canScrollHorizontal = layoutManager.canScrollHorizontally();
        if(!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity ){return false;}
        if(velocityX < 0){
            scrollToCard(-1);
        }else{
            scrollToCard(1);
        }
        return true;
    }

    public int getItemPadding() {
        return itemPadding;
    }

    public void setItemPadding(int itemPadding) {
        this.itemPadding = itemPadding;
        wrapperAdapter.notifyDataSetChanged();
    }

    private void setWrapperAdapter(WrapperAdapter adapter) {
        super.setAdapter(wrapperAdapter = adapter);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if(adapter instanceof CardAdapter){
            setAdapter((CardAdapter)adapter);
        }else{
            throw new IllegalArgumentException("Adapter should instanceof CardAdapter");
        }
    }

    public void setAdapter(CardAdapter adapter) {
        wrapperAdapter.setAdapter(adapter);
    }

    public static abstract class CardAdapter extends Adapter{
        public float getItemWidth() {
            return 0.8f;
        }
        public void onPageChanged(int position){}
    }

    private static CardAdapter defaultCardAdapter = new CardAdapter(){
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {}
        @Override
        public int getItemCount() {
            return 0;
        }
    };

    private static class WrapperAdapter extends Adapter{
        static final int VIEW_TYPE_PH_RIGHT = 1000, VIEW_TYPE_PH_LEFT = 1001,
                VIEW_TYPE_EMPTY = 1003;
        CardAdapter adapter;
        WeakReference<CardViewPager> viewPager;

        AdapterDataObserver wrapObserver = new AdapterDataObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart+1, itemCount);
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                notifyItemRangeChanged(positionStart+1, itemCount, payload);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart+1, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart+1, itemCount);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                for(int i=0;i<itemCount;i++){
                    notifyItemMoved(fromPosition+1+i, toPosition+1+i);
                }
            }
        };

        public WrapperAdapter(CardViewPager viewPager, CardAdapter adapter) {
            this.viewPager = new WeakReference<>(viewPager);
//            this.adapter = adapter;
            setAdapter(adapter);
        }

        public void setAdapter(CardAdapter adapter) {
            if(this.adapter != null){
                this.adapter.unregisterAdapterDataObserver(wrapObserver);
            }
            this.adapter = adapter;
            if(adapter != null){
                adapter.registerAdapterDataObserver(wrapObserver);
            }else{
                setAdapter(defaultCardAdapter);
                return;
            }
            notifyDataSetChanged();
        }

        public void onPageChanged(int position){
            if(position == 0 || position == getItemCount() - 1){return;}
            this.adapter.onPageChanged(position - 1);
        }

        public float getItemWidth(int position) {
            if(getRealCount() == 0){
                if(position == 0 || position == getItemCount() - 1){
                    return 0.3f;
                }
                return 0.4f;
            }

            if(position == 0 || position == getItemCount() - 1){
                final CardViewPager cardViewPager = viewPager.get();
                final float padding = (1f * cardViewPager.getItemPadding() / cardViewPager.getWidth());
                return (1f - adapter.getItemWidth() - padding * 2) * .5f;
            }
            return adapter.getItemWidth();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CardViewPager cardViewPager = (CardViewPager) parent;

            final int hanlfPadding = (int) (cardViewPager.getItemPadding() * .5f);
            switch (viewType){
                case VIEW_TYPE_PH_LEFT:{
                    final int width = parent.getWidth();
                    final ViewHolder viewHolder = makeViewHolder(parent);
                    setWidth(viewHolder, (int) (getItemWidth(0) * width));
                    setMarginLR(viewHolder, 0, hanlfPadding);
                    return viewHolder;
                }
                case VIEW_TYPE_PH_RIGHT:{
                    final int width = parent.getWidth();
                    final ViewHolder viewHolder = makeViewHolder(parent);
                    setWidth(viewHolder, (int) (getItemWidth(0) * width));
                    setMarginLR(viewHolder, hanlfPadding, 0);
                    return viewHolder;
                }
                default:{
//                case VIEW_TYPE_ITEM:{
                    final int width = parent.getWidth();
                    final ViewHolder viewHolder = adapter.onCreateViewHolder(parent, viewType);
                    setWidth(viewHolder, (int) (width * adapter.getItemWidth()));
                    setMarginLR(viewHolder, hanlfPadding, hanlfPadding);
                    return viewHolder;
                }
                case VIEW_TYPE_EMPTY:{
                    final int width = parent.getWidth();
                    final ViewHolder viewHolder = makeViewHolder(parent);
                    setWidth(viewHolder, (int) (width * getItemWidth(1)));
                    setMarginLR(viewHolder, hanlfPadding, hanlfPadding);
                    return viewHolder;
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int viewType = getItemViewType(position);

            switch (viewType) {
                case VIEW_TYPE_PH_LEFT: {
                    CardViewPager viewPager = this.viewPager.get();
                    setWidth(holder, (int) (getItemWidth(0) * viewPager.getWidth()));
                    break;
                }
                case VIEW_TYPE_PH_RIGHT: {
                    CardViewPager viewPager = this.viewPager.get();
                    setWidth(holder, (int) (getItemWidth(0) * viewPager.getWidth()));
                    break;
                }
                default: {
//                case VIEW_TYPE_ITEM:{
                    CardViewPager viewPager = this.viewPager.get();
                    setWidth(holder, (int) (viewPager.getWidth() * adapter.getItemWidth()));
                    break;
                }
                case VIEW_TYPE_EMPTY: {
                    CardViewPager viewPager = this.viewPager.get();
                    setWidth(holder, (int) (viewPager.getWidth() * adapter.getItemWidth()));
                    break;
                }
            }

            switch (viewType){
                case VIEW_TYPE_PH_RIGHT:
                case VIEW_TYPE_PH_LEFT:
                case VIEW_TYPE_EMPTY:{
                    break;
                }
                default:{
                    adapter.onBindViewHolder(holder, position - 1);
                    break;
                }
            }
        }

        @Override
        public int getItemCount() {
            final int realCount = getRealCount();
            return 2 + (realCount == 0 ? 1 : realCount);
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0){return VIEW_TYPE_PH_LEFT;}
            if(position == getItemCount() - 1){return VIEW_TYPE_PH_RIGHT;}

            if(getRealCount() == 0){return VIEW_TYPE_EMPTY;}
            return adapter.getItemViewType(position - 1);
        }

        private int getRealCount() {
            return adapter == null ? 0 : adapter.getItemCount();
        }
    }

    //=========
    public static void setWidth(ViewHolder viewHolder, int width) {
        final View itemView = viewHolder.itemView;
        ensureLayoutParams(viewHolder);
        final RecyclerView.LayoutParams layoutParams = (LayoutParams) itemView.getLayoutParams();
        layoutParams.width = width;
        itemView.setLayoutParams(layoutParams);
    }

    public static void setHeight(ViewHolder viewHolder, int height) {
        final View itemView = viewHolder.itemView;
        ensureLayoutParams(viewHolder);
        final RecyclerView.LayoutParams layoutParams = (LayoutParams) itemView.getLayoutParams();
        layoutParams.height = height;
        itemView.setLayoutParams(layoutParams);
    }

    public static void setMarginLR(ViewHolder viewHolder, int left, int right) {
        final View itemView = viewHolder.itemView;
        ensureLayoutParams(viewHolder);
        final RecyclerView.LayoutParams layoutParams = (LayoutParams) itemView.getLayoutParams();
        layoutParams.leftMargin = left;
        layoutParams.rightMargin = right;
        itemView.setLayoutParams(layoutParams);
    }

    public static void setMarginTB(ViewHolder viewHolder, int top, int bottom) {
        final View itemView = viewHolder.itemView;
        ensureLayoutParams(viewHolder);
        final RecyclerView.LayoutParams layoutParams = (LayoutParams) itemView.getLayoutParams();
        layoutParams.topMargin = top;
        layoutParams.bottomMargin = bottom;
        itemView.setLayoutParams(layoutParams);
    }

    private static void ensureLayoutParams(ViewHolder holder) {
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if(layoutParams == null || !(layoutParams instanceof RecyclerView.LayoutParams)){
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    public static ViewHolder makeViewHolder(ViewGroup parent) {
        final ViewHolder viewHolder = new ViewHolder(makeView(parent)) {};
        return viewHolder;
    }

    public static View makeView(ViewGroup parent) {
        View view = new View(parent.getContext());
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }
}

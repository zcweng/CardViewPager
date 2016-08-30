# CardViewPager
水平卡片式滚动的RecyclerView，行为类似ViewPager，但有很多特性



```java
cardPager.setItemPadding(padding);
adapter = new MyAdapter();
cardPager.setAdapter(adapter);

class MyAdapter extends CardViewPager.CardAdapter {
  @Override
  public float getItemWidth() {
    return .8f;
  }
}
```

package com.app.swipeableitem

enum class ViewState {
    Opened,
    Closed
}

enum class ViewMoveDirection {
    StartToEnd,
    EndToStart,
    No
}

enum class TouchDirection {
    Horizontal,
    Vertical,
    No
}

enum class SwipeDirection {
    ToEnd,
    ToStart,
    Both
}

enum class AnimationType {
    Open,
    Move
}
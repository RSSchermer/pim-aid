var scrolling = false;

$(document).ready(function () {
    $("#scrollUp").bind("mousedown", function (event) {
        event.preventDefault();

        scrolling = true;
        scrollContent("up");
    }).bind("mouseup", function () {
        scrolling = false;
    });


    $("#scrollDown").bind("mousedown", function (event) {
        event.preventDefault();

        scrolling = true;
        scrollContent("down");
    }).bind("mouseup", function () {
        scrolling = false;
    });

    showScrollDown();
    showScrollUp();

    $(window).scroll(function() {
        showScrollDown();
        showScrollUp();
    });
});

function scrollContent(direction) {
    var amount = (direction === "up" ? "-=4px" : "+=4px");
    $("body").animate({
        scrollTop: amount
    }, 1, function() {
        if (scrolling) {
            scrollContent(direction);
        }
    });
}

function showScrollDown() {
    var scrollDown = $("#scrollDown");

    if($(window).scrollTop() + $(window).height() == $(document).height()) {
        if (scrollDown.css("display") !== "none") {
            scrollDown.fadeOut();
        }
    } else if (!scrollDown.is(":visible")) {
        scrollDown.fadeIn();
    }
}

function showScrollUp() {
    var scrollUp = $("#scrollUp");

    if($(window).scrollTop() == 0) {
        if (scrollUp.css("display") !== "none") {
            scrollUp.fadeOut();
        }
    } else if (!scrollUp.is(":visible")) {
        scrollUp.fadeIn();
    }
}

// TODO:
// - create canvas
// - draw characters

var SCREEN_WIDTH = 73;
var SCREEN_HEIGHT = 60;
var SCREEN_RATIO = SCREEN_WIDTH / SCREEN_HEIGHT;

function init_canvas() {
    var canvas = document.createElement("canvas");
    canvas.id     = "asciiCanvas";
    document.getElementById("screen").appendChild(canvas);
    return canvas;
}

function resize_canvas(canvas) {
    if (window.innerWidth / window.innerHeight > SCREEN_RATIO) {
        canvas.height = window.innerHeight;
        canvas.width = SCREEN_RATIO * canvas.height;
    } else {
        canvas.width = window.innerWidth;
        canvas.height = canvas.width / SCREEN_RATIO;
    }
}

function update_canvas(asciiImage) {
    var start = new Date().getTime();

    var canvas = document.getElementById("asciiCanvas");
    var ctx = canvas.getContext("2d");

    resize_canvas(canvas);
    var w = canvas.width;
    var h = canvas.height;

    ctx.clearRect(0, 0, w, h);

    var dx = w / SCREEN_WIDTH;
    var dy = h / SCREEN_HEIGHT;
    var y0 = dy - 2;
    var n = asciiImage.colors.length;

    ctx.font = "bold " + Math.floor(dy + 3) + "px monospace";
    var x = 0;
    var y = 0;
    for (var i = 0; i < n; ++i) {
        ctx.fillStyle = "#" + asciiImage.colors[i];
        ctx.fillText(asciiImage.chars.charAt(i), x * dx, y0 + y * dy);
        ++x;
        if (x == SCREEN_WIDTH) {
            x = 0;
            ++y;
        }
    }
    var ms = (new Date().getTime() - start);
    console.log("Render time: " + ms + "ms (" + Math.floor(1000 / ms) + "fps)");

}

function decode_decompress(base64) {
    // Decode Base64
    var raw = atob(base64);
    // Create empty byte arary
    var binData = new Uint8Array(new ArrayBuffer(raw.length));
    // Fill with data
    for (i = 0; i < raw.length; ++i) {
        binData[i] = raw.charCodeAt(i);
    }
    // Zlib decompress
    var data = pako.inflate(binData);
    // Back to string
    return String.fromCharCode.apply(null, new Uint16Array(data));
}

function init_websocket(queue) {
    var socket = new WebSocket("ws://localhost:5000/ascii");

    socket.onopen = function(event) {
        console.log("Connected!", event);
    };

    socket.onmessage = function(event) {

        var start = new Date().getTime();
            var json = decode_decompress(event.data);
            var asciiImage = JSON.parse(json);
        var ms = (new Date().getTime() - start);
        queue.push(asciiImage);
        console.log("Decompress time: " + ms + "ms (" + Math.floor(1000 / ms) + "fps)");
    };
}

function init_loop(queue) {
    setInterval(function(queue) {
        var frame = queue.shift();
        if (typeof frame !== "undefined") {
            var refresh = function() { update_canvas(frame); };
            window.requestAnimationFrame(refresh);
        }
    }, 33, queue);
}

init_canvas();

var queue = [];
init_websocket(queue);
init_loop(queue);

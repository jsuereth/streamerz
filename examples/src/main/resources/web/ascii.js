// TODO:
// - create canvas
// - draw characters

var SCREEN_WIDTH = 80;
var SCREEN_HEIGHT = 60;
var SCREEN_RATIO = SCREEN_WIDTH / SCREEN_HEIGHT;

var received_message_example = "eAHtnUtKA1EQRbcSGjMxEvL/QaD3IQ46PxwIAR2Ke/eFSsuBR+kCPG9QXKrEQSpd5/arQT6b4/Xt+v7R7J6byWTSPP0Rp9Np+ZsszmazUs3ifD4v1SwuFotSzeJyuSzVLK5Wq1LN4nq9LtUsbjabUs3idrst1Sx2XVeqWTwcDqWaxePxWKpZPJ1OpZrF8/lcqlm8XC6lWkf7W3c562zks85GPuts5LPORj7rbOSzzka+7mxk7K/9zSZz5LPJHPlsMkc+m8yRzyZz5LPJHPlsMke+7ikzPr/suP3lxHY+84n2+SWR5S8ntvOZjkt/RSKTtrWWv/KXzKWWv/KXzKWWv/KXzKWWv/I3u8sibWvt+6/8JXOpff+Vv2Qute+/8pfMpa7feZnx/ZcUlr/yl8yllr/yl8yllr/yl8ylJm1rLX/lL5lL7f2z/CVzqb1/lr9kLrX3z/LX++fwWvor/RU9FbX+Sn9FT0Wtv9Jf0VNR66/0V/or/RWdVWj3R/oreipq90f6K3oqavdH+it6Kup6Z8SM9xuksPyVv2QutfyVv2QutfyVv2QuNWlba/krf8lcavcL8pfMpXa/IH/JXGr3C/LX/UJ4Lf2V/oqeilp/pb+ip6LWX+mv6Kmo9Vf6K/2V/orOKrT7I/0VPRW1+yP9FT0Vtfsj/RU9FXW9M2LG+w1SWP7KXzKXWv7KXzKXWv7KXzKXmrSttfyVv2QutfsF+UvmUrtfkL9kLrX7hf/I35fyY3iv3e2XCJvB/Yz78xRn15/Hn7OPM9qP4jz0ZxinHbZt6//z87t/Y/y+3B8Qn4/BoB8vY+dLP1B9Pnw+5KXzwHmgn7xZBP3zzSQ4D5wHzgPnQf/C4DxwHvwyD5qvb2DF0fo=";
var received_message_example2 = "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";

function test() {

    init_canvas();

    var json = decode_decompress(received_message_example2);
    var asciiImage = JSON.parse(json);
    update_canvas(asciiImage, 0);

}

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

function update_canvas(asciiImage, offset) {
    var start = new Date().getTime();

    var canvas = document.getElementById("asciiCanvas");
    var ctx = canvas.getContext("2d");

    resize_canvas(canvas);
    var w = canvas.width;
    var h = canvas.height;

//    ctx.fillStyle = "#000";
//    ctx.fillRect(0, 0, w, h);
    ctx.clearRect(0, 0, w, h);

    var dx = w / SCREEN_WIDTH;
    var dy = h / SCREEN_HEIGHT;
    var y0 = dy - 2;
    var n = asciiImage.colors.length;

    ctx.font = "bold " + Math.floor(dy + 3) + "px monospace";
    var x = 0;
    var y = 0;
    for (var i = 0; i < n; ++i) {
        var j = (i + offset) % n;
        ctx.fillStyle = "#" + asciiImage.colors[j];
        ctx.fillText(asciiImage.chars.charAt(j), x * dx, y0 + y * dy);
        ++x;
        if (x == SCREEN_WIDTH) {
            x = 0;
            ++y;
        }
    }
    var ms = (new Date().getTime() - start);
    console.log("Render time: " + ms + "ms (" + Math.floor(1000 / ms) + "fps)");

    var refresh = function() {update_canvas(asciiImage, offset+1)};
    window.requestAnimationFrame(refresh);
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

function init_websocket() {
    var socket = new WebSocket("ws://localhost:5000/ascii");

    socket.onopen = function(event) {
        console.log("Connected!", event);
    };

    socket.onmessage = function(event) {
        var json = decode_decompress(event.data);
        var asciiImage = JSON.parse(json);
        var refresh = function() { update_canvas(canvas, asciiImage); };
        window.requestAnimationFrame(refresh);
    };
}

test();

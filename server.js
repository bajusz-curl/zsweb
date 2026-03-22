const express = require("express");
const fetch = (...a) => import("node-fetch").then(({ default: f }) => f(...a));
const cheerio = require("cheerio");
const { URL } = require("url");

const app = express();

function abs(link, base) {
  try { return new URL(link, base).href; }
  catch { return null; }
}

app.get("/render", async (req, res) => {
  const url = req.query.url;
  if (!url) return res.send("ERROR");

  try {
    const r = await fetch(url, { redirect: "follow" });
    const html = await r.text();
    const $ = cheerio.load(html);

    let out = "";

    const title = $("title").text();
    if (title) out += "TITLE|" + title + "\n";

    $("p,h1,h2,h3").each((i,e)=>{
      let t=$(e).text().trim();
      if(t) out += "BODY|" + t + "\n";
    });

    $("a").each((i,e)=>{
      let href=$(e).attr("href");
      let u=abs(href,url);
      if(u) out += "LINK|" + u + "\n";
    });

    $("audio,video,source").each((i,e)=>{
      let src=$(e).attr("src");
      let u=abs(src,url);
      if(u) out += "MEDIA|" + u + "\n";
    });

    res.send(out);

  } catch (e) {
    res.send("ERROR");
  }
});

app.listen(process.env.PORT || 3000, "0.0.0.0");

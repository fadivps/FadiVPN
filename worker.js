export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/vmess") {
      const response = await fetch(
        "https://api.github.com/repos/fadivps/FadiVPN/contents/config/vmess.json?ref=master",
        {
          headers: {
            "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
            "Accept": "application/vnd.github.raw+json",
            "User-Agent": "FadiVPN-Updater"
          }
        }
      );

      if (!response.ok) {
        return new Response(
          `GitHub error: ${response.status}`,
          { status: 502 }
        );
      }

      const data = await response.text();

      return new Response(data, {
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          "Cache-Control": "no-store"
        }
      });
    }

    return new Response("FadiVPN Updater OK");
  }
};

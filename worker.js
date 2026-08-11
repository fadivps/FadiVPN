export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname === "/vmess") {
      const response = await fetch(
        "https://raw.githubusercontent.com/fadivps/FadiVPN/master/config/vmess.json",
        {
          headers: {
            "User-Agent": "FadiVPN-Updater"
          }
        }
      );

      if (!response.ok) {
        return new Response("VMess configuration unavailable", {
          status: 502
        });
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

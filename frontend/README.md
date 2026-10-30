# P2P Ledger frontend

The React/Vite frontend provides login and account registration for the Spring
Boot API.

## Run locally

From this directory, install dependencies and start the development server:

```sh
npm install
npm run dev
```

The Vite development server proxies `/login` and `/register` to
`http://localhost:8080` by default. Set `VITE_API_PROXY_TARGET` in a local
`.env` file to change that backend URL.

For a deployed frontend, set `VITE_API_BASE_URL` to the API origin before
building. A successful login stores the plain-text JWT response in
`sessionStorage` as `p2pLedgerToken`.

Run `npm run build` to create the production bundle in `dist/`.

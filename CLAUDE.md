# HomeMatch SG (SC2006 Team ABCDE)

## Rules
- Never add "Co-authored-by: Claude" or any Claude attribution to commits or PRs.
- Never put matric numbers in this repo.

## Stack and commands
- backend/: Spring Boot 3.3, Java 17. Run: .\mvnw.cmd spring-boot:run  Test: .\mvnw.cmd test
- frontend/: React 18 + TypeScript + Vite + Tailwind. Run: npm run dev  Check: npm run typecheck; npm test
- Browser walkthrough: qa/e2e.mjs (needs both servers running)

## Gotchas
- Dark mode is a patch layer at the bottom of frontend/src/index.css, not Tailwind dark: classes.
  Any new colour class (bg-*, text-*) needs a .dark override there or it breaks contrast.
- Keep class names matching the Lab 3 design model (ExternalApiGateway, CachingApiGatewayProxy, etc.).
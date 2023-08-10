#stage 1
FROM node:16-alpine as node
WORKDIR /app
COPY . /app/
RUN npm install
RUN npm run build --prod

#stage 2
FROM nginx:alpine
#RUN rm -rf /usr/share/nginx/html/*
COPY --from=node /app/dist/angular /usr/share/nginx/html
COPY ./nginx/default.conf /etc/nginx/nginx.conf

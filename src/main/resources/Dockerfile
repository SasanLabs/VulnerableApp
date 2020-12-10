FROM openresty/openresty:alpine

COPY nginx.conf /usr/local/openresty/nginx/conf
COPY ./static /usr/share/nginx/html/

# COPY test.lua /usr/local/openresty/nginx/test.lua

# COPY required.lua /usr/local/openresty/lualib/required.lua